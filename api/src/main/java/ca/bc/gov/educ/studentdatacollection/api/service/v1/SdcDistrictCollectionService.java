package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SdcDistrictCollectionService {

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Autowired
  public SdcDistrictCollectionService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
  }

  public SdcDistrictCollectionEntity getSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntity =  sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
    if(sdcDistrictCollectionEntity.isPresent()) {
      return sdcDistrictCollectionEntity.get();
    } else {
      throw new EntityNotFoundException(SdcDistrictCollectionEntity.class, "SdcDistrictCollection for sdcDistrictCollectionID", sdcDistrictCollectionID.toString());
    }
  }

  public SdcDistrictCollectionEntity getActiveSdcDistrictCollectionByDistrictID(UUID districtID) {
    return sdcDistrictCollectionRepository.findByDistrictIDAndSdcDistrictCollectionStatusCodeNotIgnoreCase(districtID, SdcDistrictCollectionStatus.COMPLETED.getCode()).orElseThrow(() -> new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection for district Id", districtID.toString()));
  }
}
