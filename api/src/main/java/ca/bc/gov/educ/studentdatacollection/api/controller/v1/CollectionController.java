package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.CollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.CollectionPayloadValidator;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
public class CollectionController implements CollectionEndpoint {

  private static final CollectionMapper collectionMapper = CollectionMapper.mapper;

  private final CollectionPayloadValidator collectionPayloadValidator;
  private final CollectionService collectionService;

  @Autowired
  public CollectionController(final CollectionService collectionService, final CollectionPayloadValidator collectionPayloadValidator) {
    this.collectionService = collectionService;

    this.collectionPayloadValidator = collectionPayloadValidator;
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

  private void validatePayload(Supplier<List<FieldError>> validator) {
    val validationResult = validator.get();
    if (!validationResult.isEmpty()) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }

}