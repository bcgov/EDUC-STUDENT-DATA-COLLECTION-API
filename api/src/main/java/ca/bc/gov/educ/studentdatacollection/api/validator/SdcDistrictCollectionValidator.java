package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
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
public class SdcDistrictCollectionValidator {

  public static final String DISTRICT_COLLECTION_ID = "sdcDistrictCollectionID";
  public static final String DISTRICT_COLLECTION_STATUS_CODE = "sdcDistrictCollectionStatusCode";
  private static final String DISTRICT_COLLECTION_OBJECT_NAME = "sdcDistrictCollection";

  @Getter(AccessLevel.PRIVATE)
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Autowired
  public SdcDistrictCollectionValidator(SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
  }

  public List<FieldError> validatePayload(SdcDistrictCollection sdcDistrictCollection, boolean isCreateOperation) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    try {
      if(isCreateOperation && sdcDistrictCollection.getSdcDistrictCollectionID() != null) {
        apiValidationErrors.add(ValidationUtil.createFieldError(DISTRICT_COLLECTION_OBJECT_NAME, DISTRICT_COLLECTION_ID, sdcDistrictCollection.getSdcDistrictCollectionID(), "sdcDistrictCollectionID should be null for post operation."));
      }
      if(!isCreateOperation) {
        Optional<SdcDistrictCollectionEntity> districtCollectionEntity = this.sdcDistrictCollectionRepository.findById(UUID.fromString(sdcDistrictCollection.getSdcDistrictCollectionID()));
        if (districtCollectionEntity.isEmpty()) {
          apiValidationErrors.add(ValidationUtil.createFieldError(DISTRICT_COLLECTION_OBJECT_NAME, DISTRICT_COLLECTION_ID, sdcDistrictCollection.getSdcDistrictCollectionID(), "Invalid SDC district collection ID."));
        }
      }
      if (!EnumUtils.isValidEnum(SdcDistrictCollectionStatus.class, sdcDistrictCollection.getSdcDistrictCollectionStatusCode())) {
        apiValidationErrors.add(ValidationUtil.createFieldError(DISTRICT_COLLECTION_OBJECT_NAME, DISTRICT_COLLECTION_STATUS_CODE, sdcDistrictCollection.getSdcDistrictCollectionStatusCode(), "Invalid SDC district collection status code."));
      }
    }catch(Exception e){
      apiValidationErrors.add(ValidationUtil.createFieldError(DISTRICT_COLLECTION_OBJECT_NAME, DISTRICT_COLLECTION_STATUS_CODE, sdcDistrictCollection.getSdcDistrictCollectionStatusCode(), "Invalid SDC district collection status code."));
    }
    return apiValidationErrors;
  }
}
