package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionStudentEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    private static final SdcSchoolCollectionStudentMapper mapper = SdcSchoolCollectionStudentMapper.mapper;

    @Override
    public SdcSchoolCollectionStudent getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        return mapper.toSdcSchoolCollectionStudentWithValidationIssues(this.sdcSchoolCollectionStudentService.getSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID));
    }

    @Override
    public SdcSchoolCollectionStudentValidationIssueErrorWarningCount getErrorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
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
    public SdcSchoolCollectionStudent updateAndValidateSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudentID, sdcSchoolCollectionStudent));
        RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
        return mapper.toSdcSchoolCollectionStudentWithValidationIssues(sdcSchoolCollectionStudentService.updateAndValidateSdcSchoolCollectionStudent
                (mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent)));
    }

    @Override
    public SdcSchoolCollectionStudent deleteSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        SdcSchoolCollectionStudentEntity softDeletedSdcSchoolCollectionStudent = this.sdcSchoolCollectionStudentService.softDeleteSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID);
        return mapper.toSdcSchoolStudent(softDeletedSdcSchoolCollectionStudent);
    }

    @Override
    public List<SdcStudentEll> createYearsInEll(List<SdcStudentEll> studentElls) {
        return this.sdcSchoolCollectionStudentService.createOrReturnSdcStudentEll(studentElls);
    }

    @Override
    public SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(UUID sdcSchoolCollectionID, String type, boolean compare) {
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(sdcSchoolCollectionID).orElseThrow(() ->
                new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));
        switch (type) {
            case "enrollment":
                return sdcSchoolCollectionStudentHeadcountService.getEnrollmentHeadcounts(sdcSchoolCollectionEntity, compare);
            case "french":
                return sdcSchoolCollectionStudentHeadcountService.getFrenchHeadcounts(sdcSchoolCollectionEntity, compare);
            case "ell":
                return sdcSchoolCollectionStudentHeadcountService.getEllHeadcounts(sdcSchoolCollectionEntity, compare);
            case "career":
                return sdcSchoolCollectionStudentHeadcountService.getCareerHeadcounts(sdcSchoolCollectionEntity, compare);
            case "indigenous":
                return sdcSchoolCollectionStudentHeadcountService.getIndigenousHeadcounts(sdcSchoolCollectionEntity, compare);
            case "special-ed":
                return sdcSchoolCollectionStudentHeadcountService.getSpecialEdHeadcounts(sdcSchoolCollectionEntity, compare);
            default:
                log.error("Invalid type for getSdcSchoolCollectionStudentHeadcounts::" + type);
                throw new InvalidParameterException();
        }
    }

}
