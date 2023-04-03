package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.CollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.CollectionPayloadValidator;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

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
  public Collection getCollectionBySchoolId(UUID schoolID) {
    return collectionMapper.toStructure(this.collectionService.getCollectionBySchoolId(schoolID));
  }

  @Override
  public List<Collection> getAllCollections() {
    return this.collectionService.getAllCollectionsList().stream().map(collectionMapper::toStructure).toList();
  }

  @Override
  public Collection createCollection(Collection collection) throws JsonProcessingException {
    ValidationUtil.validatePayload(() -> this.collectionPayloadValidator.validateCreatePayload(collection));
    RequestUtil.setAuditColumnsForCreate(collection);

    return collectionMapper.toStructure(collectionService.createCollection(collection));
  }

  @Override
  @Transactional
  public ResponseEntity<Void> deleteCollection(UUID collectionID) {
    this.collectionService.deleteCollection(collectionID);
    return ResponseEntity.noContent().build();
  }

}
