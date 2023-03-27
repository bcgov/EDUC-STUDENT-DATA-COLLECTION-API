package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class CollectionPayloadValidator {
  
  public static final String COLLECTION_TYPE_CODE = "collectionTypeCode";
  
  @Getter(AccessLevel.PRIVATE)
  private final CollectionService collectionService;

  @Getter(AccessLevel.PRIVATE)
  private final CodeTableService codeTableService;

  @Autowired
  public CollectionPayloadValidator(final CollectionService collectionService, final CodeTableService codeTableService) {
    this.collectionService = collectionService;
    this.codeTableService = codeTableService;
  }

  public List<FieldError> validatePayload(Collection collection, boolean isCreateOperation) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    if (isCreateOperation && collection.getCollectionID() != null) {
      apiValidationErrors.add(ValidationUtil.createFieldError("collectionID", collection.getCollectionID(), "collectionID should be null for post operation."));
    }
    validateCollectionCodePayload(collection, apiValidationErrors);

    return apiValidationErrors;
  }

  public List<FieldError> validateCreatePayload(Collection collection) {
    return validatePayload(collection, true);
  }

  protected void validateCollectionCodePayload(Collection collection, List<FieldError> apiValidationErrors) {
    if (collection.getCollectionTypeCode() != null) {
      Optional<CollectionTypeCodeEntity> collectionCodeEntity = codeTableService.getCollectionCode(collection.getCollectionTypeCode());
      if (collectionCodeEntity.isEmpty()) {
        apiValidationErrors.add(ValidationUtil.createFieldError(COLLECTION_TYPE_CODE, collection.getCollectionTypeCode(), "Invalid collection code."));
      } else if (collectionCodeEntity.get().getEffectiveDate() != null && collectionCodeEntity.get().getEffectiveDate().isAfter(
          LocalDateTime.now())) {
        apiValidationErrors.add(ValidationUtil.createFieldError(COLLECTION_TYPE_CODE, collection.getCollectionTypeCode(), "Collection Code provided is not yet effective."));
      } else if (collectionCodeEntity.get().getExpiryDate() != null && collectionCodeEntity.get().getExpiryDate().isBefore(LocalDateTime.now())) {
        apiValidationErrors.add(
          ValidationUtil.createFieldError(COLLECTION_TYPE_CODE, collection.getCollectionTypeCode(),
                "Collection Code provided has expired."));
      }
    }
  }

}
