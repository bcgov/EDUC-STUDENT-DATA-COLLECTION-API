package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.service.v1.CollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

@Component
public class CollectionPayloadValidator {
  @Getter(AccessLevel.PRIVATE)
  private final CollectionService collectionService;

  @Autowired
  public CollectionPayloadValidator(final CollectionService collectionService) {
    this.collectionService = collectionService;
  }

  public List<FieldError> validatePayload(Collection collection, boolean isCreateOperation) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    if (isCreateOperation && collection.getCollectionID() != null) {
      apiValidationErrors.add(createFieldError("collectionID", collection.getCollectionID(), "collectionID should be null for post operation."));
    }

    return apiValidationErrors;
  }

  public List<FieldError> validateCreatePayload(Collection collection) {
    return validatePayload(collection, true);
  }

  private FieldError createFieldError(String fieldName, Object rejectedValue, String message) {
    return new FieldError("collection", fieldName, rejectedValue, false, null, null, message);
  }

}
