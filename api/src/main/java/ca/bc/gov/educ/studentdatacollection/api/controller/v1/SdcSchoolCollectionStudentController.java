package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionStudentEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.filter.SdcSchoolCollectionStudentFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssueErrorWarningCount;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentController implements SdcSchoolCollectionStudentEndpoint {

    private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;

    private static final SdcSchoolCollectionStudentMapper mapper = SdcSchoolCollectionStudentMapper.mapper;

    @Override
    public SdcSchoolCollectionStudent getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
        return mapper.toSdcSchoolCollectionStudentWithValidationIssues(this.sdcSchoolCollectionStudentService.getSdcSchoolCollectionStudent(sdcSchoolCollectionStudentID));
    }

    @Override
    public SdcSchoolCollectionStudentValidationIssueErrorWarningCount ErrorAndWarningCountBySdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        return this.sdcSchoolCollectionStudentService.errorAndWarningCountBySdcSchoolCollectionID(sdcSchoolCollectionID);
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public CompletableFuture<Page<SdcSchoolCollectionStudent>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentEntity> studentSpecs = sdcSchoolCollectionStudentSearchService
                .setSpecificationAndSortCriteria(
                        sortCriteriaJson,
                        searchCriteriaListJson,
                        JsonUtil.mapper,
                        sorts
                );
        return this.sdcSchoolCollectionStudentSearchService
                .findAll(studentSpecs, pageNumber, pageSize, sorts)
                .thenApplyAsync(sdcSchoolStudentEntities -> sdcSchoolStudentEntities.map(mapper::toSdcSchoolStudent));
    }

    @Override
    public ResponseEntity<Void> updateAndValidateSdcSchoolCollectionStudent(UUID collectionStudentID, SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
        sdcSchoolCollectionStudentService.updateAndValidateSdcSchoolCollectionStudent(mapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
