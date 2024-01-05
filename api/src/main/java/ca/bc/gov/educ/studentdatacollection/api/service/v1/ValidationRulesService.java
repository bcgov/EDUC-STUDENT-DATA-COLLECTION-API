package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
        final List<String> enrolledProgramCodes = splitString(enrolledProgramCode);
        var activeEnrolledCodes = getActiveEnrolledProgramCodes().stream().map(EnrolledProgramCode::getEnrolledProgramCode).toList();
        return !enrolledProgramCodes.stream().allMatch(activeEnrolledCodes::contains);
    }

    public List<String> splitString(String enrolledProgramCode) {
        if (StringUtils.isEmpty(enrolledProgramCode)) {
            return Collections.<String>emptyList();
        }
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }

    public Optional<SdcStudentEllEntity> getStudentYearsInEll(String studentID) {
        return sdcStudentEllRepository.findByStudentID(UUID.fromString(studentID));
    }

    public void updatePenMatchAndGradStatusColumns(SdcSchoolCollectionStudentEntity student, String mincode) throws EntityNotFoundException {
        var penMatchResult = this.restUtils.getPenMatchResult(UUID.randomUUID(), student, mincode);
        val penMatchResultCode = penMatchResult.getPenStatus();
        student.setPenMatchResult(penMatchResultCode);
        var validPenMatchResults = Arrays.asList("AA", "B1", "C1", "D1");
        var multiPenMatchResults = Arrays.asList("BM", "CM", "DM");

        if (StringUtils.isNotEmpty(penMatchResultCode) && validPenMatchResults.contains(penMatchResultCode)) {
            if (penMatchResult.getMatchingRecords() == null) {
                log.error("PEN Match records list is null - this should not have happened :: ", penMatchResult);
                throw new StudentDataCollectionAPIRuntimeException("PEN Match records list is null - this should not have happened :: ");
            }
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
        } else if (StringUtils.isNotEmpty(penMatchResultCode) && multiPenMatchResults.contains(penMatchResultCode)) {
            student.setPenMatchResult("MULTI");
        } else {
            student.setPenMatchResult("NEW");
        }

//    if(student.getAssignedStudentId() != null) {
//        final var gradResult = this.restUtils.getGradStatusResult(UUID.randomUUID(), SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(student));
//        log.info("Grad status for SDC student {} :: is {}", student.getSdcSchoolCollectionStudentID(), gradResult);
//    }
        student.setIsGraduated(false);
    }

    public boolean hasEnrollmentHistory(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        var school = studentRuleData.getSchool();
        var twoYearsAgo = student.getSdcSchoolCollection().getCollectionEntity().getOpenDate().getYear() - 2;


        var listOfNumCoursesLastTwoYears = getSdcSchoolStudentRepository().getCollectionHistory(UUID.fromString(school.getSchoolId()),
                student.getStudentPen(), student.getSdcSchoolCollection().getCollectionEntity().getOpenDate(), studentRuleData.getCollectionTypeCode(), twoYearsAgo);

        for (String numString : listOfNumCoursesLastTwoYears) {
            try {
                if (Integer.parseInt(numString) > 0) {
                    return true;
                }
            } catch (Exception e) {
                //Do nothing
            }
        }

        return false;
    }
}
