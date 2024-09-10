package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.HeadcountReportTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionStudentEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssueErrorWarningCount;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentEll;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentController implements SdcSchoolCollectionStudentEndpoint {

    private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;

    private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;

    private final SdcSchoolCollectionStudentHeadcountService sdcSchoolCollectionStudentHeadcountService;

    private final SdcDuplicatesService sdcDuplicatesService;

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
    public CompletableFuture<Slice<SdcSchoolCollectionStudent>> findAllSlice(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs = sdcSchoolCollectionStudentSearchService
                .setSpecificationAndSortCriteria(
                        sortCriteriaJson,
                        searchCriteriaListJson,
                        JsonUtil.mapper,
                        sorts
                );
        return this.sdcSchoolCollectionStudentSearchService
                .findAllSlice(studentSpecs, pageNumber, pageSize, sorts)
                .thenApplyAsync(sdcSchoolStudentEntities -> sdcSchoolStudentEntities.map(mapper::toSdcSchoolCollectionStudentWithValidationIssues));
    }

    @Override
    public SdcSchoolCollectionStudent createAndUpdateSdcSchoolCollectionStudent(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudent));
         if(StringUtils.isNotBlank(sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentID())) {
             RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
            return mapper.toSdcSchoolCollectionStudentWithValidationIssues(sdcDuplicatesService.routeSdcSchoolCollectionStudentUpdate
                    (mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent), false));
        } else {
             RequestUtil.setAuditColumnsForCreate(sdcSchoolCollectionStudent);
            return mapper.toSdcSchoolCollectionStudentWithValidationIssues(sdcDuplicatesService.createSdcSchoolCollectionStudent
                    (mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent)));
        }

    }

    @Override
    public SdcSchoolCollectionStudent deleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        SdcSchoolCollectionStudentEntity softDeletedSdcSchoolCollectionStudent = this.sdcDuplicatesService.softDeleteSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID);
        return mapper.toSdcSchoolStudent(softDeletedSdcSchoolCollectionStudent);
    }

    @Override
    public List<SdcSchoolCollectionStudent> softDeleteSdcSchoolCollectionStudents(List<UUID> sdcStudentIDs) {
        return this.sdcDuplicatesService.softDeleteSdcSchoolCollectionStudents(sdcStudentIDs).stream().map(mapper::toSdcSchoolStudent).toList();
    }

    @Override
    public List<SdcStudentEll> createYearsInEll(List<SdcStudentEll> studentElls) {
        return this.sdcSchoolCollectionStudentService.createOrReturnSdcStudentEll(studentElls);
    }

    @Override
    public SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(UUID sdcSchoolCollectionID, String type, boolean compare) {
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID).orElseThrow(() ->
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

    @Override
    public ResponseEntity<Void> updatePENStatus(String penCode, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudent));
        RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
        if(penCode.equalsIgnoreCase("NEW")) {
            sdcSchoolCollectionStudentService.updatePenStatusToNEW(mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));
            return new ResponseEntity<>(HttpStatus.OK);
        } else if(penCode.equalsIgnoreCase("MATCH")) {
            sdcSchoolCollectionStudentService.updatePenStatusToMATCH(mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return null;
    }


}
