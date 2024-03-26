package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDistrictCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDistrictCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class SdcDistrictCollectionController implements SdcDistrictCollectionEndpoint {

  private static final SdcDistrictCollectionMapper mapper = SdcDistrictCollectionMapper.mapper;
  private final SdcDistrictCollectionService sdcDistrictCollectionService;

  public SdcDistrictCollectionController(SdcDistrictCollectionService sdcDistrictCollectionService) {
    this.sdcDistrictCollectionService = sdcDistrictCollectionService;
  }

  @Override
  public SdcDistrictCollection getAllActiveDistrictCollectionsByDistrictId(UUID districtID) {
    SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionService.getAllActiveSdcDistrictCollectionByDistrictID(districtID);
    return mapper.toStructure(sdcDistrictCollectionEntity);
  }
}
