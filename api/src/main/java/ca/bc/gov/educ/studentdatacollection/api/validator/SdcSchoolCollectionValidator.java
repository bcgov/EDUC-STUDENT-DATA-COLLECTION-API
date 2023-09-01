package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class SdcSchoolCollectionValidator {

  public static final String SCHOOL_COLLECTION_ID = "sdcSchoolCollectionID";
  public static final String SCHOOL_COLLECTION_STATUS_CODE = "sdcSchoolCollectionStatusCode";

  @Getter(AccessLevel.PRIVATE)
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Autowired
  public SdcSchoolCollectionValidator(SdcSchoolCollectionRepository sdcSchoolCollectionRepository) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
  }

  public List<FieldError> validatePayload(SdcSchoolCollection sdcSchoolCollection, boolean isCreateOperation) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    try {

      if(isCreateOperation && sdcSchoolCollection.getSdcSchoolCollectionID() != null) {
        apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, sdcSchoolCollection.getSdcSchoolCollectionID(), "sdcSchoolCollectionID should be null for post operation."));
      }

      if(!isCreateOperation) {
        Optional<SdcSchoolCollectionEntity> schoolCollectionEntity = this.sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolCollection.getSdcSchoolCollectionID()));
        if (schoolCollectionEntity.isEmpty()) {
          apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_ID, sdcSchoolCollection.getSdcSchoolCollectionID(), "Invalid SDC school collection ID."));
        }
      }

      if (!EnumUtils.isValidEnum(SdcSchoolCollectionStatus.class, sdcSchoolCollection.getSdcSchoolCollectionStatusCode())) {
        apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_STATUS_CODE, sdcSchoolCollection.getSdcSchoolCollectionStatusCode(), "Invalid SDC school collection status code."));
      }
    }catch(Exception e){
      apiValidationErrors.add(ValidationUtil.createFieldError(SCHOOL_COLLECTION_STATUS_CODE, sdcSchoolCollection.getSdcSchoolCollectionStatusCode(), "Invalid SDC school collection status code."));
    }

    return apiValidationErrors;
  }

}
