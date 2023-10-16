package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cglib.core.Local;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class ValidationRulesService {
    private final CodeTableService codeTableService;
    private final SdcStudentEllRepository sdcStudentEllRepository;
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Getter(PRIVATE)
    private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;
    private final RestUtils restUtils;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;
    public ValidationRulesService(CodeTableService codeTableService, SdcStudentEllRepository sdcStudentEllRepository, SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, RestUtils restUtils) {
        this.codeTableService = codeTableService;
        this.sdcStudentEllRepository = sdcStudentEllRepository;
        this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
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
    public  List<BandCode> getActiveBandCodes() {
        return codeTableService.getAllBandCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public  List<SchoolFundingCode> getActiveFundingCodes() {
        return codeTableService.getAllFundingCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<EnrolledGradeCode> getActiveGradeCodes() {
        return codeTableService.getAllEnrolledGradeCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<SpecialEducationCategoryCode> getActiveSpecialEducationCategoryCodes() {
        return codeTableService.getAllSpecialEducationCategoryCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public boolean isEnrolledProgramCodeInvalid(String enrolledProgramCode) {
        final List<String> enrolledProgramCodes = splitString(enrolledProgramCode);
        var activeEnrolledCodes = getActiveEnrolledProgramCodes().stream().map(EnrolledProgramCode::getEnrolledProgramCode).toList();
        return !enrolledProgramCodes.stream().allMatch(activeEnrolledCodes::contains);
    }

    public List<String> splitString(String enrolledProgramCode) {
        if(StringUtils.isEmpty(enrolledProgramCode)) {
            return Collections.<String>emptyList();
        }
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }

    public Optional<SdcStudentEllEntity> getStudentYearsInEll(String studentID){
        return sdcStudentEllRepository.findByStudentID(UUID.fromString(studentID));
    }

  public void updatePenMatchAndGradStatusColumns(SdcSchoolCollectionStudentEntity student, String mincode) throws EntityNotFoundException {
    var penMatchResult = this.restUtils.getPenMatchResult(UUID.randomUUID(), student, mincode);
    val penMatchResultCode = penMatchResult.getPenStatus();
    student.setPenMatchResult(penMatchResultCode);
    var validPenMatchResults = Arrays.asList("AA", "B1", "C1", "D1");

    if (StringUtils.isNotEmpty(penMatchResultCode) && validPenMatchResults.contains(penMatchResultCode)) {
      final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
      if (penMatchRecordOptional.isPresent()) {
        var assignedPEN = penMatchRecordOptional.get().getMatchingPEN();
        var assignedStudentID = penMatchRecordOptional.get().getStudentID();

        student.setAssignedStudentId(UUID.fromString(assignedStudentID));
        student.setAssignedPen(assignedPEN);
      } else {
        log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
        throw new StudentDataCollectionAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      }
    }else{
      student.setPenMatchResult("NEW");
    }

    //TODO Change me
    if(student.getIsGraduated() == null) {
      student.setIsGraduated(false);
    }
  }

  public void updateStudentAgeColumns(SdcSchoolCollectionStudentEntity studentEntity){
    String studentDOB = studentEntity.getDob();
    studentEntity.setIsAdult(DOBUtil.isAdult(studentDOB));
    studentEntity.setIsSchoolAged(DOBUtil.isSchoolAged(studentDOB));
  }

  public boolean hasNoEnrollmentHistory(StudentRuleData studentRuleData){
      var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
      var school = studentRuleData.getSchool();

      var twoYearAgoCreateDate = sdcSchoolCollectionRepository.getCollectionHistoryStartDate(UUID.fromString(school.getSchoolId()), studentRuleData.getCollectionTypeCode(), student.getSdcSchoolCollection().getCollectionEntity().getOpenDate(), 2);
      if (twoYearAgoCreateDate == null){
          twoYearAgoCreateDate = studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollection().getCollectionEntity().getOpenDate().minusYears(2);
      }

      var listOfNumCoursesLastTwoYears = getSdcSchoolStudentRepository().getCollectionHistory(UUID.fromString(school.getSchoolId()), student.getStudentPen(), twoYearAgoCreateDate);
      for (String numString : listOfNumCoursesLastTwoYears){
          if (Integer.parseInt(numString) > 0){
              return false;
          }
      }

      return true;
    }
}
