package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.CollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.CloseCollectionOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionSearchService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionService;

import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;

import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.CollectionPayloadValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
public class CollectionController implements CollectionEndpoint {

  private static final CollectionMapper collectionMapper = CollectionMapper.mapper;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private static final SdcSchoolCollectionMapper sdcSchoolCollectionMapper = SdcSchoolCollectionMapper.mapper;
  private static final SdcDistrictCollectionMapper sdcDistrictCollectionMapper = SdcDistrictCollectionMapper.mapper;
  private final CollectionPayloadValidator collectionPayloadValidator;
  private final CollectionService collectionService;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final CloseCollectionOrchestrator closeCollectionOrchestrator;
  private final SagaService sagaService;
  private final CollectionSearchService collectionSearchService;

  @Autowired
  public CollectionController(final CollectionService collectionService, final CollectionPayloadValidator collectionPayloadValidator, final SdcDuplicatesService sdcDuplicatesService, CloseCollectionOrchestrator closeCollectionOrchestrator, SagaService sagaService, CollectionSearchService collectionSearchService) {
    this.collectionService = collectionService;
    this.sdcDuplicatesService = sdcDuplicatesService;
    this.collectionPayloadValidator = collectionPayloadValidator;
    this.closeCollectionOrchestrator = closeCollectionOrchestrator;
    this.sagaService = sagaService;
    this.collectionSearchService = collectionSearchService;
  }

  @Override
  public Collection getCollection(UUID collectionID) {
    Optional<CollectionEntity> collection = this.collectionService.getCollection(collectionID);

    if (collection.isPresent()) {
      return collectionMapper.toStructure(collection.get());
    } else {
      throw new EntityNotFoundException();
    }
  }

  @Override
  public List<Collection> getCollections(String createUser) {
    List<CollectionEntity> collections = this.collectionService.getCollections(createUser);

    List<Collection> collectionList = new ArrayList<>();
    for(CollectionEntity entity: collections){
      collectionList.add(collectionMapper.toStructure(entity));
    }

    return collectionList;
  }

  @Override
  public Collection getActiveCollection(){
    return CollectionMapper.mapper.toStructure(collectionService.getActiveCollection());
  }

  @Override
  public Collection createCollection(Collection collection) {
    validatePayload(() -> this.collectionPayloadValidator.validateCreatePayload(collection));
    RequestUtil.setAuditColumnsForCreate(collection);

    return collectionMapper.toStructure(collectionService.createCollection(collection));
  }

  @Override
  @Transactional
  public ResponseEntity<Void> deleteCollection(UUID collectionID) {
    this.collectionService.deleteCollection(collectionID);
    return ResponseEntity.noContent().build();
  }

  @Override
  public List<MonitorSdcDistrictCollection> getMonitorSdcDistrictCollectionResponse(UUID collectionId) {
    return this.collectionService.getMonitorSdcDistrictCollectionResponse(collectionId);
  }

  @Override
  public MonitorIndySdcSchoolCollectionsResponse getMonitorIndySdcSchoolCollectionResponse(UUID collectionId) {
    return this.collectionService.getMonitorIndySdcSchoolCollectionResponse(collectionId);
  }

  @Override
  public List<String> findDuplicatesInCollection(UUID collectionID, List<String> matchedAssignedIDs) {
    return this.collectionService.findDuplicatesInCollection(collectionID, matchedAssignedIDs);
  }

  private void validatePayload(Supplier<List<FieldError>> validator) {
    val validationResult = validator.get();
    if (!validationResult.isEmpty()) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }

  @Override
  public ResponseEntity<Void> generateProvinceDuplicates(UUID collectionID) {
    this.sdcDuplicatesService.generateAllProvincialDuplicates(collectionID);
    return ResponseEntity.ok().build();
  }

  @Override
  public List<SdcDuplicate> getProvinceDuplicates(UUID collectionID) {
    return this.sdcDuplicatesService.getAllProvincialDuplicatesByCollectionID(collectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public ResponseEntity<String> closeCollection(CollectionSagaData collectionSagaData) throws JsonProcessingException {
    final var sagaInProgress = this.sagaService.findByCollectionIDAndSagaNameAndStatusNot(UUID.fromString(collectionSagaData.getExistingCollectionID()), SagaEnum.CLOSE_COLLECTION_SAGA.toString(), SagaStatusEnum.COMPLETED.toString());
    if (sagaInProgress.isPresent()) {
      return ResponseEntity.status(HttpStatus.CONFLICT).build();
    } else {
      RequestUtil.setAuditColumnsForCreate(collectionSagaData);
      val saga = this.closeCollectionOrchestrator.createSaga(JsonUtil.getJsonStringFromObject(collectionSagaData), null, null, ApplicationProperties.STUDENT_DATA_COLLECTION_API, UUID.fromString(collectionSagaData.getExistingCollectionID()));
      log.info("Starting closeCollectionOrchestrator orchestrator :: {}", saga);
      this.closeCollectionOrchestrator.startSaga(saga);
      return ResponseEntity.status(HttpStatus.ACCEPTED).body(saga.getSagaId().toString());
    }
  }
  @Override
  public ResponseEntity<Void> resolveRemainingDuplicates(UUID collectionID){
    this.sdcDuplicatesService.resolveRemainingDuplicates(collectionID);
    return ResponseEntity.ok().build();
  }

  @Override
  public CompletableFuture<Page<Collection>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
    final List<Sort.Order> sorts = new ArrayList<>();
    Specification<CollectionEntity> collectionSpecs = collectionSearchService
            .setSpecificationAndSortCriteria(
                    sortCriteriaJson,
                    searchCriteriaListJson,
                    JsonUtil.mapper,
                    sorts
            );
    return this.collectionSearchService
            .findAll(collectionSpecs, pageNumber, pageSize, sorts)
            .thenApplyAsync(collectionEntities -> collectionEntities.map(collectionMapper::toStructure));
  }

  @Override
  public List<SdcSchoolCollection> getSchoolCollectionsInCollection(UUID collectionID){
    List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = this.collectionService.getSchoolCollectionsInCollection(collectionID);

    List<SdcSchoolCollection> sdcSchoolCollectionList = new ArrayList<>();
    for (SdcSchoolCollectionEntity entity : sdcSchoolCollectionEntities) {
      sdcSchoolCollectionList.add(sdcSchoolCollectionMapper.toStructure(entity));
    }
    return sdcSchoolCollectionList;
  }

  @Override
  public List<SdcDistrictCollection> getDistrictCollectionsInCollection(UUID collectionID){
    List<SdcDistrictCollectionEntity> sdcDistrictCollectionEntities = this.collectionService.getDistrictCollectionsInCollection(collectionID);

    List<SdcDistrictCollection> sdcDistrictCollectionList = new ArrayList<>();
    for (SdcDistrictCollectionEntity entity : sdcDistrictCollectionEntities) {
      sdcDistrictCollectionList.add(sdcDistrictCollectionMapper.toStructure(entity));
    }
    return sdcDistrictCollectionList;
  }
}
