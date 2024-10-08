package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SdcSchoolCollectionFileValidator {

  public static final String SCHOOL_COLLECTION_ID = "sdcSchoolCollectionID";

  @Getter(AccessLevel.PRIVATE)
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  public SdcSchoolCollectionFileValidator(SdcSchoolCollectionRepository sdcSchoolCollectionRepository) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
  }

  @Transactional
  public List<FieldError> validatePayload(String sdcSchoolCollectionID) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    validateSdcFileUpload(sdcSchoolCollectionID, apiValidationErrors);

    return apiValidationErrors;
  }

  private void validateSdcFileUpload(String sdcSchoolCollectionID, List<FieldError> apiValidationErrors) {
    try {
      Optional<SdcSchoolCollectionEntity> schoolCollectionEntity = this.sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolCollectionID));
      if (schoolCollectionEntity.isEmpty()) {
        apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, sdcSchoolCollectionID, "Invalid SDC school collection ID."));
      } else {
        var sdcSchoolCollectionEntity = schoolCollectionEntity.get();
        if (!(sdcSchoolCollectionEntity.getCollectionEntity().getCollectionStatusCode().equals(CollectionStatus.INPROGRESS.getCode()))) {
          apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, sdcSchoolCollectionID, "Invalid SDC school collection ID, collection period is closed."));
        }
      }
    } catch(Exception e) {
      apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, sdcSchoolCollectionID, "Invalid SDC school collection ID."));
    }
  }
}
