package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.HeadcountReportTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionStudentEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentController implements SdcSchoolCollectionStudentEndpoint {

    private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;

    private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;

    private final SdcSchoolCollectionStudentHeadcountService sdcSchoolCollectionStudentHeadcountService;

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    private static final SdcSchoolCollectionStudentMapper mapper = SdcSchoolCollectionStudentMapper.mapper;

    @Override
    public SdcSchoolCollectionStudent getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        return mapper.toSdcSchoolCollectionStudentWithValidationIssues(this.sdcSchoolCollectionStudentService.getSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID));
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssueErrorWarningCount> getErrorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        return this.sdcSchoolCollectionStudentService.errorAndWarningCountBySdcSchoolCollectionID(sdcSchoolCollectionID);
    }

    @Override
    public CompletableFuture<Page<SdcSchoolCollectionStudent>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs = sdcSchoolCollectionStudentSearchService
            .setSpecificationAndSortCriteria(
                    sortCriteriaJson,
                    searchCriteriaListJson,
                    JsonUtil.mapper,
                    sorts
                    );
        return this.sdcSchoolCollectionStudentSearchService
            .findAll(studentSpecs, pageNumber, pageSize, sorts)
            .thenApplyAsync(sdcSchoolStudentEntities -> sdcSchoolStudentEntities.map(mapper::toSdcSchoolCollectionStudentWithValidationIssues));
    }

    @Override
    public CompletableFuture<ResponseEntity<byte[]>> findAllNotPaginated() {
        return this.sdcSchoolCollectionStudentSearchService
                .findAllNotPaginated()
                .thenApplyAsync(entities -> {
                    log.info("Start create CSV");
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(baos), CSVFormat.DEFAULT
                                 .withHeader("School Code", "School Name", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer", "Refugee",
                                         "Native Ancestry", "Native Status", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses"))) {

                        for (SdcSchoolCollectionStudentLightEntity student : entities) {
                            List<? extends Serializable> csvRow = Arrays.asList(
                                    student.getSdcSchoolCollectionStudentID(),
                                    student.getSdcSchoolCollectionStudentID(),
                                    student.getStudentPen(),
                                    student.getLegalFirstName() + " " + student.getLegalLastName(),
                                    student.getUsualFirstName() + " " + student.getUsualLastName(),
                                    student.getDob(),
                                    student.getGender(),
                                    student.getPostalCode(),
                                    student.getLocalID(),
                                    student.getEnrolledGradeCode(),
                                    student.getFte(),
                                    student.getIsAdult(),
                                    student.getIsGraduated(),
                                    student.getIsGraduated(),
                                    student.getIsGraduated(),
                                    student.getNativeAncestryInd(),
                                    student.getNativeAncestryInd(),
                                    student.getSdcSchoolCollectionStudentStatusCode(),
                                    student.getBandCode(),
                                    student.getHomeLanguageSpokenCode(),
                                    student.getNumberOfCourses(),
                                    student.getSupportBlocks(),
                                    student.getOtherCourses()
                            );
                            csvPrinter.printRecord(csvRow);
                        }

                        csvPrinter.flush();

                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sdcSchoolCollectionStudents.csv");
                        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

                        log.info("Finish create CSV");
                        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to generate CSV", e);
                    }
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<byte[]>> findAllNonPaginated(String sortCriteriaJson, String searchCriteriaListJson) {
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs = sdcSchoolCollectionStudentSearchService
                .setSpecificationAndSortCriteria(
                        sortCriteriaJson,
                        searchCriteriaListJson,
                        JsonUtil.mapper,
                        sorts
                );
        return this.sdcSchoolCollectionStudentSearchService
                //.findAllNonPaginated(studentSpecs, sorts)
                .findAll(studentSpecs, 0, 400, sorts)
                .thenApplyAsync(entities -> {
                    log.info("Start create CSV");
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                         CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(baos), CSVFormat.DEFAULT
                                 .withHeader("School Code", "School Name", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer", "Refugee",
                                         "Native Ancestry", "Native Status", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses"))) {

                        for (SdcSchoolCollectionStudentPaginationEntity student : entities) {
                            List<? extends Serializable> csvRow = Arrays.asList(
                                    student.getSdcSchoolCollectionStudentID(),
                                    student.getSdcSchoolCollectionStudentID(),
                                    student.getStudentPen(),
                                    student.getLegalFirstName() + " " + student.getLegalLastName(),
                                    student.getUsualFirstName() + " " + student.getUsualLastName(),
                                    student.getDob(),
                                    student.getGender(),
                                    student.getPostalCode(),
                                    student.getLocalID(),
                                    student.getEnrolledGradeCode(),
                                    student.getFte(),
                                    student.getIsAdult(),
                                    student.getIsGraduated(),
                                    student.getIsGraduated(),
                                    student.getIsGraduated(),
                                    student.getNativeAncestryInd(),
                                    student.getNativeAncestryInd(),
                                    student.getSdcSchoolCollectionStudentStatusCode(),
                                    student.getBandCode(),
                                    student.getHomeLanguageSpokenCode(),
                                    student.getNumberOfCourses(),
                                    student.getSupportBlocks(),
                                    student.getOtherCourses()
                            );
                            csvPrinter.printRecord(csvRow);
                        }

                        csvPrinter.flush();

                        HttpHeaders headers = new HttpHeaders();
                        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=sdcSchoolCollectionStudents.csv");
                        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv");

                        log.info("Finish create CSV");
                        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to generate CSV", e);
                    }
                });
    }

    @Override
    public SdcSchoolCollectionStudent createAndUpdateSdcSchoolCollectionStudent(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudent));
         if(StringUtils.isNotBlank(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())) {
             RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
            return mapper.toSdcSchoolCollectionStudentWithValidationIssues(sdcSchoolCollectionStudentService.updateSdcSchoolCollectionStudent
                    (mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent)));
        } else {
             RequestUtil.setAuditColumnsForCreate(sdcSchoolCollectionStudent);
            return mapper.toSdcSchoolCollectionStudentWithValidationIssues(sdcSchoolCollectionStudentService.createSdcSchoolCollectionStudent
                    (mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent)));
        }

    }

    @Override
    public SdcSchoolCollectionStudent deleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        SdcSchoolCollectionStudentEntity softDeletedSdcSchoolCollectionStudent = this.sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID);
        return mapper.toSdcSchoolStudent(softDeletedSdcSchoolCollectionStudent);
    }

    @Override
    public List<SdcSchoolCollectionStudent> softDeleteSdcSchoolCollectionStudents(List<UUID> sdcStudentIDs) {
        return this.sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudents(sdcStudentIDs).stream().map(mapper::toSdcSchoolStudent).toList();
    }

    @Override
    public List<SdcStudentEll> createYearsInEll(List<SdcStudentEll> studentElls) {
        return this.sdcSchoolCollectionStudentService.createOrReturnSdcStudentEll(studentElls);
    }

    @Override
    public SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(UUID sdcSchoolCollectionID, String type, boolean compare) {
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(sdcSchoolCollectionID).orElseThrow(() ->
                new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));
        if (HeadcountReportTypeCodes.ENROLLMENT.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getEnrollmentHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.FRENCH.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getFrenchHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.ELL.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getEllHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.CAREER.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getCareerHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.INDIGENOUS.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getIndigenousHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.SPECIAL_ED.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getSpecialEdHeadcounts(sdcSchoolCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.BAND_CODES.getCode().equals(type)) {
            return sdcSchoolCollectionStudentHeadcountService.getBandResidenceHeadcounts(sdcSchoolCollectionEntity, compare);
        } else {
            log.error("Invalid type for getSdcSchoolCollectionStudentHeadcounts::" + type);
            throw new InvalidParameterException(type);
        }
    }

}
