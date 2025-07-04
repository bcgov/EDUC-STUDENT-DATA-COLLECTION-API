package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class ValidationRulesService {
    private final CodeTableService codeTableService;
    private final SdcStudentEllRepository sdcStudentEllRepository;
    @Getter(PRIVATE)
    private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;
    private final RestUtils restUtils;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public ValidationRulesService(CodeTableService codeTableService, SdcStudentEllRepository sdcStudentEllRepository, SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository, RestUtils restUtils) {
        this.codeTableService = codeTableService;
        this.sdcStudentEllRepository = sdcStudentEllRepository;
        this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
        this.restUtils = restUtils;
    }

    public Long getDuplicatePenCount(UUID sdcSchoolID, String studentPen) {
        return sdcSchoolStudentRepository.countForDuplicateStudentPENs(sdcSchoolID, studentPen);
    }

    public List<EnrolledProgramCode> getActiveEnrolledProgramCodes() {
        return codeTableService.getAllEnrolledProgramCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<CareerProgramCode> getActiveCareerProgramCodes() {
        return codeTableService.getAllCareerProgramCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<HomeLanguageSpokenCode> getActiveHomeLanguageSpokenCodes() {
        return codeTableService.getAllHomeLanguageSpokenCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<BandCode> getActiveBandCodes() {
        return codeTableService.getAllBandCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<SchoolFundingCode> getActiveFundingCodes() {
        return codeTableService.getAllFundingCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<EnrolledGradeCode> getActiveGradeCodes() {
        return codeTableService.getAllEnrolledGradeCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<SpecialEducationCategoryCode> getActiveSpecialEducationCategoryCodes() {
        return codeTableService.getAllSpecialEducationCategoryCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public boolean isEnrolledProgramCodeInvalid(String enrolledProgramCode) {
        final List<String> enrolledProgramCodes = splitEnrolledProgramsString(enrolledProgramCode);
        var activeEnrolledCodes = getActiveEnrolledProgramCodes().stream().map(EnrolledProgramCode::getEnrolledProgramCode).toList();
        return !enrolledProgramCodes.stream().allMatch(activeEnrolledCodes::contains);
    }

    public List<String> splitEnrolledProgramsString(String enrolledProgramCode) {
        if (StringUtils.isEmpty(enrolledProgramCode)) {
            return Collections.<String>emptyList();
        }
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }

    public Optional<SdcStudentEllEntity> getStudentYearsInEll(UUID studentID) {
        return sdcStudentEllRepository.findByStudentID(studentID);
    }

    public List<IndependentSchoolFundingGroup> getSchoolFundingGroups(String schoolID) {
        return restUtils.getSchoolFundingGroupsBySchoolID(schoolID);
    }

    public void setupMergedStudentIdValues(StudentRuleData studentRuleData) {
        setupPENMatchAndEllAndGraduateValues(studentRuleData);
        var mergedStudentIds = studentRuleData.getHistoricStudentIds();
        if(mergedStudentIds == null && studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId() != null) {
            mergedStudentIds = new ArrayList<>(
                this.restUtils.getMergedStudentIds(UUID.randomUUID(), studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId()).stream()
                    .map(studentMerge -> UUID.fromString(studentMerge.getMergeStudentID())).toList());
            mergedStudentIds.add(studentRuleData.getSdcSchoolCollectionStudentEntity().getAssignedStudentId());
            studentRuleData.setHistoricStudentIds(mergedStudentIds);
        }
    }

    public void setupPENMatchAndEllAndGraduateValues(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        if(student.getPenMatchResult() == null){
            runAndSetPenMatch(student, studentRuleData.getSchool().getMincode());
        }
        if(student.getIsGraduated() == null){
            setGraduationStatus(student);
            setStudentYearsInEll(student);
        }
    }

    public void runAndSetPenMatch(SdcSchoolCollectionStudentEntity student, String mincode) throws EntityNotFoundException {
        var penMatchResult = this.restUtils.getPenMatchResult(UUID.randomUUID(), student, mincode);
        val penMatchResultCode = penMatchResult.getPenStatus();
        var validPenMatchResults = Arrays.asList("AA", "B1", "C1", "D1");
        var multiPenMatchResults = Arrays.asList("BM", "CM", "DM");

        if (StringUtils.isNotEmpty(penMatchResultCode) && validPenMatchResults.contains(penMatchResultCode)) {
            setPositivePENMatch(penMatchResult, student);
        } else if (StringUtils.isNotEmpty(penMatchResultCode) && multiPenMatchResults.contains(penMatchResultCode)) {
            student.setPenMatchResult("MULTI");
        } else {
            student.setPenMatchResult("CONFLICT");
            if(StringUtils.isNotEmpty(penMatchResultCode) && penMatchResultCode.equalsIgnoreCase("F1")){
                if(StringUtils.isNotBlank(student.getLegalLastName()) && StringUtils.countMatches(student.getLegalLastName(), " ") == 1){
                    //Split the first name and try again
                    try {
                        var clonedStudent = new SdcSchoolCollectionStudentEntity();
                        BeanUtils.copyProperties(clonedStudent, student);
                        String[] parts = clonedStudent.getLegalLastName().split(" ");
                        clonedStudent.setLegalFirstName(parts[0]);
                        clonedStudent.setLegalLastName(parts[1]);

                        var penMatchResultF1 = this.restUtils.getPenMatchResult(UUID.randomUUID(), clonedStudent, mincode);
                        if (StringUtils.isNotEmpty(penMatchResultF1.getPenStatus()) && validPenMatchResults.contains(penMatchResultF1.getPenStatus())) {
                            setPositivePENMatch(penMatchResultF1, student);
                        }
                    } catch (Exception e) {
                        //Keep the F1
                    }
                }
            }
        }
    }

    private void setPositivePENMatch(PenMatchResult penMatchResult, SdcSchoolCollectionStudentEntity student){
        if (penMatchResult.getMatchingRecords() == null || penMatchResult.getMatchingRecords().isEmpty()) {
            log.error("PEN Match records list is null or empty - this should not have happened :: {}", penMatchResult);
            throw new StudentDataCollectionAPIRuntimeException("PEN Match records list is null or empty - this should not have happened");
        }
        final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
        if (penMatchRecordOptional.isPresent()) {
            var assignedPEN = penMatchRecordOptional.get().getMatchingPEN();
            var assignedStudentID = penMatchRecordOptional.get().getStudentID();
            student.setPenMatchResult("MATCH");
            student.setAssignedStudentId(UUID.fromString(assignedStudentID));
            student.setAssignedPen(assignedPEN);
        }
    }

    public void setStudentYearsInEll(SdcSchoolCollectionStudentEntity student){
        student.setYearsInEll(null);
        if(student.getAssignedStudentId() != null) {
            final var yearsInEll = this.getStudentYearsInEll(student.getAssignedStudentId());
            log.debug("Student years in ELL found for SDC student {} :: is {}", student.getSdcSchoolCollectionStudentID(), yearsInEll);
            if(yearsInEll.isPresent()){
                student.setYearsInEll(yearsInEll.get().getYearsInEll());
            }else{
                student.setYearsInEll(0);
            }
        }
    }

    public void setGraduationStatus(SdcSchoolCollectionStudentEntity student){
        student.setIsGraduated(false);
        if(student.getAssignedStudentId() != null) {
            final var gradResult = this.restUtils.getGradStatusResult(UUID.randomUUID(), SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(student));
            log.debug("Grad status for SDC student {} :: is {}", student.getSdcSchoolCollectionStudentID(), gradResult);
            if(StringUtils.isNotEmpty(gradResult.getException()) && gradResult.getException().equalsIgnoreCase("error")){
                log.error("Exception occurred calling grad service for grad status - this should not have happened :: {}", gradResult);
                throw new StudentDataCollectionAPIRuntimeException("Exception occurred calling grad service for grad status - this should not have happened");
            }else if(StringUtils.isNotEmpty(gradResult.getProgramCompletionDate())
                    && (StringUtils.isEmpty(gradResult.getProgram())
                    || !gradResult.getProgram().equalsIgnoreCase("SCCP"))){
                try{
                    LocalDate programCompletionDate = LocalDate.parse(gradResult.getProgramCompletionDate(), formatter);
                    if(programCompletionDate.isBefore(student.getSdcSchoolCollection().getCollectionEntity().getSnapshotDate()) ||
                            programCompletionDate.isEqual(student.getSdcSchoolCollection().getCollectionEntity().getSnapshotDate())){
                        student.setIsGraduated(true);
                    }
                }catch(Exception e){
                    log.error("Exception occurred calling trying to parse program completion date - this should not have happened :: {}", gradResult);
                    throw new StudentDataCollectionAPIRuntimeException("Exception occurred calling trying to parse program completion date - this should not have happened");
                }
            }
        }
    }

     public List<SdcSchoolCollectionStudentEntity> getStudentInHistoricalCollectionWithInSameDistrict(StudentRuleData studentRuleData, String noOfCollectionsForLookup) {
        setupMergedStudentIdValues(studentRuleData);
        var collection = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity();
        return sdcSchoolStudentRepository.findStudentInCurrentFiscalWithInSameDistrict(UUID.fromString(studentRuleData.getSchool().getDistrictId()), studentRuleData.getHistoricStudentIds(), noOfCollectionsForLookup, collection.getCollectionID(), collection.getSnapshotDate());
    }

    public List<SdcSchoolCollectionStudentEntity> getStudentInHistoricalCollectionWithInSameAuthority(StudentRuleData studentRuleData, String noOfCollectionsForLookup) {
        setupMergedStudentIdValues(studentRuleData);
        List<SchoolTombstone> allSchools = this.restUtils.getAllSchoolTombstones();
        List<UUID> independentSchoolIDsWithSameAuthorityID = allSchools.stream()
                .filter(school -> school.getIndependentAuthorityId() != null && school.getIndependentAuthorityId().equals(studentRuleData.getSchool().getIndependentAuthorityId()))
                .map(school -> UUID.fromString(school.getSchoolId()))
                .toList();
        return sdcSchoolStudentRepository.findStudentInCurrentFiscalWithInSameAuthority(independentSchoolIDsWithSameAuthorityID, studentRuleData.getHistoricStudentIds(), noOfCollectionsForLookup);
    }

    public List<SdcSchoolCollectionStudentEntity> findStudentInCurrentFiscal(StudentRuleData studentRuleData, String noOfCollectionsForLookup) {
        setupMergedStudentIdValues(studentRuleData);
        var collection = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity();
        return sdcSchoolStudentRepository.findStudentInCurrentFiscal(studentRuleData.getHistoricStudentIds(), noOfCollectionsForLookup, collection.getCollectionID(), collection.getSnapshotDate());
    }

    public List<SdcSchoolCollectionStudentEntity> getStudentInHistoricalCollectionInAllDistrict(StudentRuleData studentRuleData) {
        setupMergedStudentIdValues(studentRuleData);
        String noOfCollectionsForLookup = "3";
        return sdcSchoolStudentRepository.findStudentInCurrentFiscalInAllDistrict(studentRuleData.getHistoricStudentIds(), noOfCollectionsForLookup);
    }

    public List<SdcSchoolCollectionStudentEntity> getStudentInCurrentCollectionInAllDistrict(UUID assignedStudentID, UUID collectionID, UUID sdcSchoolCollectionIDToExclude) {
        return sdcSchoolStudentRepository.findStudentInCurrentCollectionInAllDistrict(assignedStudentID, collectionID, sdcSchoolCollectionIDToExclude);
    }

    public boolean studentExistsInCurrentFiscalInGrade8Or9(StudentRuleData studentRuleData) {
        setupMergedStudentIdValues(studentRuleData);
        String noOfCollectionsForLookup = "3";
        List<SdcSchoolCollectionStudentEntity> entity = sdcSchoolStudentRepository.findStudentInCurrentFiscalInGrade8Or9(studentRuleData.getHistoricStudentIds(), noOfCollectionsForLookup);
        return !entity.isEmpty();
    }
}
