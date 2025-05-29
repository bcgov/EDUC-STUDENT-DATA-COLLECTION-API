package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.ReportGenerationEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.reports.*;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionHistoryService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.summary.StudentDifference;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationController implements ReportGenerationEndpoint {

    private final GradeEnrollmentHeadcountReportService gradeEnrollmentHeadcountReportService;
    private final CareerProgramHeadcountReportService careerProgramHeadcountReportService;
    private final FrenchProgramHeadcountReportService frenchProgramHeadcountReportService;
    private final FrenchPerSchoolHeadcountReportService frenchPerSchoolHeadcountReportService;
    private final IndigenousHeadcountReportService indigenousHeadcountReportService;
    private final IndigenousPerSchoolHeadcountReportService indigenousPerSchoolHeadcountReportService;
    private final EllHeadcountReportService ellHeadcountReportService;
    private final EllHeadcountPerSchoolReportService ellHeadcountPerSchoolReportService;
    private final SpecialEdHeadcountReportService specialEdHeadcountReportService;
    private final SpecialEdHeadcountPerSchoolReportService specialEdHeadcountPerSchoolReportService;
    private final SpecialEdCategoryHeadcountPerSchoolReportService inclusiveEdCategoryHeadcountPerSchoolReportService;
    private final AllStudentLightCollectionGenerateCsvService allStudentLightCollectionGenerateCsvService;
    private final BandOfResidenceHeadcountReportService bandOfResidenceHeadcountReportService;
    private final BandOfResidenceHeadcountPerSchoolReportService bandOfResidenceHeadcountPerSchoolReportService;
    private final GradeEnrollmentHeadcountPerSchoolReportService gradeEnrollmentHeadcountPerSchoolReportService;
    private final CareerProgramHeadcountPerSchoolReportService careerProgramHeadcountPerSchoolReportService;
    private final RefugeeHeadcountPerSchoolReportService refugeeHeadcountPerSchoolReportService;
    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;
    private final SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;
    private final ZeroFTEHeadCountReportService zeroFTEHeadCountReportService;
    private static final SdcSchoolCollectionStudentMapper sdcSchoolCollectionStudentMapper = SdcSchoolCollectionStudentMapper.mapper;

    @Override
    public DownloadableReportResponse generateSDCSchoolReport(UUID sdcSchoolCollectionID, String reportTypeCode) {
        Optional<SchoolReportTypeCode> code = SchoolReportTypeCode.findByValue(reportTypeCode);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case GRADE_ENROLLMENT_HEADCOUNT -> gradeEnrollmentHeadcountReportService.generateSchoolGradeEnrollmentHeadcountReport(sdcSchoolCollectionID);
            case CAREER_HEADCOUNT -> careerProgramHeadcountReportService.generateSchoolCareerProgramHeadcountReport(sdcSchoolCollectionID);
            case FRENCH_HEADCOUNT -> frenchProgramHeadcountReportService.generateSchoolFrenchProgramHeadcountReport(sdcSchoolCollectionID);
            case INDIGENOUS_HEADCOUNT -> indigenousHeadcountReportService.generateSchoolIndigenousHeadcountReport(sdcSchoolCollectionID);
            case BAND_RESIDENCE_HEADCOUNT -> bandOfResidenceHeadcountReportService.generateSchoolBandOfResidenceReport(sdcSchoolCollectionID);
            case ELL_HEADCOUNT -> ellHeadcountReportService.generateSchoolEllHeadcountReport(sdcSchoolCollectionID);
            case SPECIAL_EDUCATION_HEADCOUNT -> specialEdHeadcountReportService.generateSchoolSpecialEdHeadcountReport(sdcSchoolCollectionID);
            case ALL_STUDENT_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_ERRORS_WARNS_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateErrorWarnInfoReportFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_FRENCH_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateFrenchFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_CAREER_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateCareerFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_INDIGENOUS_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateIndigenousFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_INCLUSIVE_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateInclusiveFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            case ALL_STUDENT_ELL_SCHOOL_CSV -> allStudentLightCollectionGenerateCsvService.generateEllFromSdcSchoolCollectionID(sdcSchoolCollectionID);
            default -> new DownloadableReportResponse();
        };
    }

    @Override
    public DownloadableReportResponse generateSDCDistrictReport(UUID sdcDistrictCollectionID, String reportTypeCode) {
        Optional<DistrictReportTypeCode> code = DistrictReportTypeCode.findByValue(reportTypeCode);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        return switch (code.get()) {
            case DIS_GRADE_ENROLLMENT_HEADCOUNT -> gradeEnrollmentHeadcountReportService.generateDistrictGradeEnrollmentHeadcountReport(sdcDistrictCollectionID);
            case DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL -> gradeEnrollmentHeadcountPerSchoolReportService.generatePerSchoolReport(sdcDistrictCollectionID);
            case DIS_CAREER_HEADCOUNT -> careerProgramHeadcountReportService.generateDistrictCareerProgramHeadcountReport(sdcDistrictCollectionID);
            case DIS_CAREER_HEADCOUNT_PER_SCHOOL -> careerProgramHeadcountPerSchoolReportService.generateCareerProgramHeadcountPerSchoolReport(sdcDistrictCollectionID);
            case DIS_FRENCH_HEADCOUNT -> frenchProgramHeadcountReportService.generateDistrictFrenchProgramHeadcountReport(sdcDistrictCollectionID);
            case DIS_FRENCH_HEADCOUNT_PER_SCHOOL -> frenchPerSchoolHeadcountReportService.generatePerSchoolReport(sdcDistrictCollectionID);
            case DIS_INDIGENOUS_HEADCOUNT -> indigenousHeadcountReportService.generateDistrictIndigenousHeadcountReport(sdcDistrictCollectionID);
            case DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL -> indigenousPerSchoolHeadcountReportService.generateIndigenousHeadcountPerSchoolReport(sdcDistrictCollectionID);
            case DIS_REFUGEE_HEADCOUNT_PER_SCHOOL -> refugeeHeadcountPerSchoolReportService.generateRefugeePerSchoolReport(sdcDistrictCollectionID);
            case DIS_ELL_HEADCOUNT -> ellHeadcountReportService.generateDistrictEllHeadcountReport(sdcDistrictCollectionID);
            case DIS_ELL_HEADCOUNT_PER_SCHOOL -> ellHeadcountPerSchoolReportService.generateEllHeadcountPerSchoolReport(sdcDistrictCollectionID);
            case DIS_SPECIAL_EDUCATION_HEADCOUNT -> specialEdHeadcountReportService.generateDistrictSpecialEdHeadcountReport(sdcDistrictCollectionID);
            case DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL -> specialEdHeadcountPerSchoolReportService.generateSpecialEdHeadcountPerSchoolReport(sdcDistrictCollectionID);
            case DIS_SPECIAL_EDUCATION_HEADCOUNT_CATEGORY_PER_SCHOOL-> inclusiveEdCategoryHeadcountPerSchoolReportService.generateInclusiveEdCategoryHeadcountPerSchoolReport(sdcDistrictCollectionID);
            case ALL_STUDENT_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_ERRORS_WARNS_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateErrorWarnInfoReportFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_FRENCH_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateFrenchFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_CAREER_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateCareerFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_INDIGENOUS_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateIndigenousFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_INCLUSIVE_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateInclusiveFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case ALL_STUDENT_ELL_DIS_CSV -> allStudentLightCollectionGenerateCsvService.generateEllFromSdcDistrictCollectionID(sdcDistrictCollectionID);
            case DIS_ZERO_FTE_SUMMARY -> zeroFTEHeadCountReportService.generateZeroFTEHeadcountReport(sdcDistrictCollectionID);
            case DIS_BAND_RESIDENCE_HEADCOUNT -> bandOfResidenceHeadcountReportService.generateDistrictBandOfResidenceReport(sdcDistrictCollectionID);
            case DIS_BAND_RESIDENCE_HEADCOUNT_PER_SCHOOL -> bandOfResidenceHeadcountPerSchoolReportService.generateBandOfResidenceHeadcountPerSchoolReport(sdcDistrictCollectionID);
            default -> new DownloadableReportResponse();
        };
    }

    @Override
    public Page<StudentDifference> getStudentDifferences(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs = sdcSchoolCollectionStudentSearchService
                .setSpecificationAndSortCriteria(
                        sortCriteriaJson,
                        searchCriteriaListJson,
                        JsonUtil.mapper,
                        sorts
                );
        try {
            var studentsWithDiffAndCriteria = this.sdcSchoolCollectionStudentSearchService.findAll(studentSpecs, pageNumber, pageSize, sorts).get();
            var currentStudentsMap = studentsWithDiffAndCriteria.stream().collect(Collectors.toMap(
                    stud -> stud.getSdcSchoolCollectionStudentID(),
                    stud -> stud
            ));
            var historyRecords = sdcSchoolCollectionHistoryService.getFirstHistoryRecordsForStudentIDs(currentStudentsMap.keySet());
            var historyRecordsMap = historyRecords.stream().collect(Collectors.toMap(
                    stud -> stud.getSdcSchoolCollectionStudentID(),
                    stud -> stud
            ));
            return getDifferencesList(currentStudentsMap, historyRecordsMap, studentsWithDiffAndCriteria.getPageable(), studentsWithDiffAndCriteria.getTotalElements());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new StudentDataCollectionAPIRuntimeException("Error occurred making pagination call: " + e.getMessage());
        } catch (Exception e) {
            throw new StudentDataCollectionAPIRuntimeException("Error occurred making pagination call: " + e.getMessage());
        }
    }

    private Page<StudentDifference> getDifferencesList(Map<UUID, SdcSchoolCollectionStudentPaginationEntity> currentStudents, Map<UUID, SdcSchoolCollectionStudentHistoryEntity> originalStudents, Pageable pageable, long total) {
        List<StudentDifference> differences = new ArrayList<>();
        originalStudents.values().stream().forEach(stud -> {
            StudentDifference diff = new StudentDifference();
            diff.setOriginalStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudentHistory(stud));
            diff.setCurrentStudent(sdcSchoolCollectionStudentMapper.toSdcSchoolStudent(currentStudents.get(stud.getSdcSchoolCollectionStudentID())));
            differences.add(diff);
        });
        return new PageImpl<>(differences, pageable, total);
    }

}
