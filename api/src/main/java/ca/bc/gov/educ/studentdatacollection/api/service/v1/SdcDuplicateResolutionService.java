package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EdxUser;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SoftDeleteRecordSet;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcDuplicateResolutionService {

  private final SdcDuplicateRepository sdcDuplicateRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final ValidationRulesService validationRulesService;
  private final DuplicateClassNumberGenerationService duplicateClassNumberGenerationService;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;
  private final SdcSchoolCollectionStudentStorageService sdcSchoolCollectionStudentStorageService;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final ScheduleHandlerService scheduleHandlerService;
  private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private final RestUtils restUtils;
  private static final String SDC_DUPLICATE_ID_KEY = "sdcDuplicateID";
  private static final String COLLECTION_ID_NOT_ACTIVE_MSG = "Provided collectionID does not match currently active collectionID.";
  private static final List<String> independentSchoolCategoryCodes = Arrays.asList(SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode(), SchoolCategoryCodes.OFFSHORE.getCode());
  private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void resolveRemainingDuplicates(UUID collectionID){
    Optional<CollectionEntity> activeCollection = collectionRepository.findActiveCollection();

    if(activeCollection.isPresent()){
      if(!activeCollection.get().getCollectionID().equals(collectionID)) {
        throw new InvalidParameterException(COLLECTION_ID_NOT_ACTIVE_MSG);
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.INPROGRESS.getCode())){
        throw new InvalidParameterException("Provided collectionID has not yet run provincial duplicates.");
      }else if(activeCollection.get().getCollectionStatusCode().equals(CollectionStatus.DUPES_RES.getCode())){
        throw new InvalidParameterException("Provided collectionID has already resolved all duplicates.");
      }
    }

    List<SdcSchoolCollectionStudentLightEntity> provinceDupes = sdcDuplicatesService.findAllInProvinceDuplicateStudentsInCollection(collectionID);
    List<SdcDuplicateEntity> finalDuplicatesSet = sdcDuplicatesService.generateFinalDuplicatesSet(provinceDupes, DuplicateLevelCode.PROVINCIAL);

    List<SdcDuplicateEntity> nonAllowableDupes = finalDuplicatesSet.stream().filter(duplicate ->
            (duplicate.getDuplicateSeverityCode().equals(DuplicateSeverityCode.NON_ALLOWABLE.getCode()) &&
                    duplicate.getDuplicateTypeCode().equals(DuplicateTypeCode.ENROLLMENT.getCode()))).toList();
    resolveEnrolmentDuplicates(nonAllowableDupes);
    List<SdcDuplicateEntity> programDupes = finalDuplicatesSet.stream().filter(duplicate ->
            duplicate.getDuplicateTypeCode().equals(DuplicateTypeCode.PROGRAM.getCode())).toList();
    resolveProgramDuplicates(programDupes);

    CollectionEntity collection = collectionRepository.findById(collectionID).orElse(null);
    if (collection != null) {
      collection.setCollectionStatusCode(String.valueOf(CollectionStatus.DUPES_RES.getCode()));
      collectionRepository.save(collection);
    }

    List<SdcSchoolCollectionEntity> schoolCollections = sdcSchoolCollectionRepository.findUncompletedSchoolCollections(collectionID);
    sdcSchoolCollectionService.updateSchoolCollectionStatuses(schoolCollections, SdcSchoolCollectionStatus.COMPLETED.getCode());

    if (collection != null && collection.getSignOffDueDate() != null) {
      sendDistrictSignoffNotificationEmails(collectionID, formatter.format(collection.getSignOffDueDate()));
    }
  }

  private void sendDistrictSignoffNotificationEmails(UUID collectionID, String signOffDueDate) {
    List<SdcDistrictCollectionEntity> incompleteDistrictCollections = sdcDistrictCollectionRepository.findAllIncompleteDistrictCollections(collectionID);
    Map<UUID, Set<String>> districtCollectionEmailMap = new HashMap<>();
    for (SdcDistrictCollectionEntity districtCollection : incompleteDistrictCollections) {
      UUID districtID = districtCollection.getDistrictID();
      List<EdxUser> districtEdxUsers = restUtils.getEdxUsersForDistrict(districtID);
      Set<String> emails = sdcDuplicatesService.pluckEmailAddressesFromDistrict(districtEdxUsers);
      if (!emails.isEmpty()) {
        districtCollectionEmailMap.put(districtCollection.getSdcDistrictCollectionID(), emails);
      }
    }
    if (!districtCollectionEmailMap.isEmpty()) {
      scheduleHandlerService.createAndStartDistrictSignoffNotificationEmailSagas(districtCollectionEmailMap, signOffDueDate);
    }
  }

  private void resolveEnrolmentDuplicates(List<SdcDuplicateEntity> unresolvedDupes){
    unresolvedDupes.forEach(dupe -> {
      SdcDuplicateStudentEntity studentDupe1 = dupe.getSdcDuplicateStudentEntities().stream().findFirst().orElse(null);
      SdcDuplicateStudentEntity studentDupe2 = dupe.getSdcDuplicateStudentEntities().stream().skip(1).findFirst().orElse(null);

      if(studentDupe1 != null && studentDupe2 != null){
        SdcSchoolCollectionEntity school1Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe1.getSdcSchoolCollectionID()).get();
        SdcSchoolCollectionEntity school2Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe2.getSdcSchoolCollectionID()).get();

        SchoolTombstone school1 = restUtils.getSchoolBySchoolID(String.valueOf(school1Collection.getSchoolID())).get();
        SchoolTombstone school2 = restUtils.getSchoolBySchoolID(String.valueOf(school2Collection.getSchoolID())).get();

        SdcSchoolCollectionStudent student1 = sdcSchoolCollectionStudentMapper.toSdcSchoolStudent(studentDupe1.getSdcSchoolCollectionStudentEntity());
        SdcSchoolCollectionStudent student2 = sdcSchoolCollectionStudentMapper.toSdcSchoolStudent(studentDupe2.getSdcSchoolCollectionStudentEntity());

        Integer dupe1ClassNum = duplicateClassNumberGenerationService.generateDuplicateClassNumber(studentDupe1.getSdcDuplicateStudentID(), school1.getFacilityTypeCode(), school1.getSchoolCategoryCode(), student1.getEnrolledGradeCode());
        Integer dupe2ClassNum = duplicateClassNumberGenerationService.generateDuplicateClassNumber(studentDupe2.getSdcDuplicateStudentID(), school2.getFacilityTypeCode(), school2.getSchoolCategoryCode(), student2.getEnrolledGradeCode());

        SdcSchoolCollectionStudent studentToRemove = identifyStudentToRemove(student1, student2, dupe1ClassNum, dupe2ClassNum, school1, school2);
        SoftDeleteRecordSet set = new SoftDeleteRecordSet();
        set.setSoftDeleteStudentIDs(Arrays.asList(UUID.fromString(studentToRemove.getSdcSchoolCollectionStudentID())));
        set.setUpdateUser("STUDENT_DATA_COLLECTION_API");
        sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudents(set);
      } else {
        throw new EntityNotFoundException(SdcDuplicateEntity.class, SDC_DUPLICATE_ID_KEY, dupe.getSdcDuplicateID().toString());
      }
    });
  }

  public SdcSchoolCollectionStudent identifyStudentToRemove(SdcSchoolCollectionStudent student1, SdcSchoolCollectionStudent student2, Integer dupe1ClassNum, Integer dupe2ClassNum, SchoolTombstone school1, SchoolTombstone school2){
    SdcSchoolCollectionStudent studentToRemove = null;

    if (dupe1ClassNum == null || dupe2ClassNum == null || dupe1ClassNum.equals(dupe2ClassNum)){
      if(student1.getFte().compareTo(student2.getFte()) > 0){
        studentToRemove = student2;
      } else if (student1.getFte().compareTo(student2.getFte()) < 0){
        studentToRemove = student1;
      } else {
        Integer institute1Number = getInstituteNumber(school1);
        Integer institute2Number = getInstituteNumber(school2);

        studentToRemove = institute1Number < institute2Number ? student2 : student1;
      }
    } else {
      studentToRemove = dupe1ClassNum.compareTo(dupe2ClassNum) < 0 ? student2 : student1;
    }

    return studentToRemove;
  }

  public void resolveProgramDuplicates(List<SdcDuplicateEntity> unresolvedDupes){
    unresolvedDupes.forEach(dupe -> {
      SdcDuplicateStudentEntity studentDupe1 = dupe.getSdcDuplicateStudentEntities().stream().findFirst().orElse(null);
      SdcDuplicateStudentEntity studentDupe2 = dupe.getSdcDuplicateStudentEntities().stream().skip(1).findFirst().orElse(null);

      if(studentDupe1 != null && studentDupe2 != null){
        SdcSchoolCollectionStudentLightEntity student1 = studentDupe1.getSdcSchoolCollectionStudentEntity();
        SdcSchoolCollectionStudentLightEntity student2 = studentDupe2.getSdcSchoolCollectionStudentEntity();

        SdcSchoolCollectionEntity school1Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe1.getSdcSchoolCollectionID()).get();
        SdcSchoolCollectionEntity school2Collection = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(studentDupe2.getSdcSchoolCollectionID()).get();

        SchoolTombstone school1 = restUtils.getSchoolBySchoolID(String.valueOf(school1Collection.getSchoolID())).get();
        SchoolTombstone school2 = restUtils.getSchoolBySchoolID(String.valueOf(school2Collection.getSchoolID())).get();

        SdcSchoolCollectionStudentLightEntity studentToEdit = identifyStudentToEdit(student1, student2, school1, school2);

        SdcSchoolCollectionStudentLightEntity updatedStudent = removeDupeProgram(studentToEdit, dupe.getProgramDuplicateTypeCode());
        sdcSchoolCollectionStudentStorageService.saveSdcStudentWithHistory(updatedStudent);
      }
    });
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void changeGrade(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent, boolean isStaffMember) {
    sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudent, isStaffMember);
  }

  public SdcSchoolCollectionStudentLightEntity identifyStudentToEdit(SdcSchoolCollectionStudentLightEntity student1, SdcSchoolCollectionStudentLightEntity student2, SchoolTombstone school1, SchoolTombstone school2){
    SdcSchoolCollectionStudentLightEntity studentToEdit = null;

    if(student1.getNumberOfCoursesDec().compareTo(student2.getNumberOfCoursesDec()) > 0){
      studentToEdit = student2;
    } else if (student1.getNumberOfCoursesDec().compareTo(student2.getNumberOfCoursesDec()) < 0){
      studentToEdit = student1;
    } else if(independentSchoolCategoryCodes.contains(school2.getSchoolCategoryCode()) && !independentSchoolCategoryCodes.contains(school1.getSchoolCategoryCode())){
      studentToEdit = student2;
    } else if (independentSchoolCategoryCodes.contains(school1.getSchoolCategoryCode()) && !independentSchoolCategoryCodes.contains(school2.getSchoolCategoryCode())){
      studentToEdit = student1;
    } else if (Objects.equals(school1.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode()) && !Objects.equals(school2.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode())){
      studentToEdit = student2;
    } else if (Objects.equals(school2.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode()) && !Objects.equals(school1.getFacilityTypeCode(), FacilityTypeCodes.STANDARD.getCode())){
      studentToEdit = student1;
    } else {
      Integer institute1Number = getInstituteNumber(school1);
      Integer institute2Number = getInstituteNumber(school2);

      if (institute1Number < institute2Number){
        studentToEdit = student2;
      } else {
        studentToEdit = student1;
      }
    }
    return studentToEdit;
  }

  private SdcSchoolCollectionStudentLightEntity removeDupeProgram(SdcSchoolCollectionStudentLightEntity student, String programDuplicateTypeCode){
    List<String> enrolledPrograms = new ArrayList<>(validationRulesService.splitEnrolledProgramsString(student.getEnrolledProgramCodes()));

    if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.CAREER.getCode())){
      List<String> studentCareerProgramCodes = EnrolledProgramCodes.getCareerProgramCodes().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentCareerProgramCodes);
      student.setCareerProgramCode(null);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.INDIGENOUS.getCode())){
      List<String> studentIndigenousProgramCodes = EnrolledProgramCodes.getIndigenousProgramCodes().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentIndigenousProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.LANGUAGE.getCode())){
      List<String> studentLanguageProgramCodes = EnrolledProgramCodes.getFrenchProgramCodesWithEll().stream().filter(enrolledPrograms::contains).toList();
      enrolledPrograms.removeAll(studentLanguageProgramCodes);
    } else if (Objects.equals(programDuplicateTypeCode, ProgramDuplicateTypeCode.SPECIAL_ED.getCode())){
      enrolledPrograms.remove(student.getSpecialEducationCategoryCode());
      student.setSpecialEducationCategoryCode(null);
    }

    student.setEnrolledProgramCodes(String.join("", enrolledPrograms));
    return student;
  }

  public Integer getInstituteNumber(SchoolTombstone school){
    Integer instituteNumber = null;
    if(independentSchoolCategoryCodes.contains(school.getSchoolCategoryCode())){
      instituteNumber = Integer.parseInt(school.getSchoolNumber());
    } else {
      Optional<District> district = restUtils.getDistrictByDistrictID(school.getDistrictId());

      if(district.isPresent()){
        instituteNumber = Integer.parseInt(district.get().getDistrictNumber());
      } else {
        throw new EntityNotFoundException(District.class, "districtID", school.getDistrictId());
      }
    }
    return instituteNumber;
  }


  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void updateStudents(List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent, boolean isStaffMember) {
    // update students
    sdcSchoolCollectionStudent.forEach(student -> {
      RequestUtil.setAuditColumnsForUpdate(student);
      sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentEntity(student), isStaffMember);
    });
  }
}
