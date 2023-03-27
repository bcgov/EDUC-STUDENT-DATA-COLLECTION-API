package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
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

  public List<FieldError> validatePayload(SdcFileUpload fileUpload) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    validateSdcFileUpload(fileUpload, apiValidationErrors);

    return apiValidationErrors;
  }

  private void validateSdcFileUpload(SdcFileUpload fileUpload, List<FieldError> apiValidationErrors) {
    try {
      Optional<SdcSchoolCollectionEntity> schoolCollectionEntity = this.sdcSchoolCollectionRepository.findById(UUID.fromString(fileUpload.getSdcSchoolCollectionID()));
      if (schoolCollectionEntity.isEmpty()) {
        apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, fileUpload.getSdcSchoolCollectionID(), "Invalid Sdc school collection ID."));
      }
    }catch(Exception e){
      apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, fileUpload.getSdcSchoolCollectionID(), "Invalid Sdc school collection ID."));
    }
  }



}
