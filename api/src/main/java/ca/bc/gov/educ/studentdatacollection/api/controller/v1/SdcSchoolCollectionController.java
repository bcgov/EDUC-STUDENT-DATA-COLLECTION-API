package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
public class SdcSchoolCollectionController implements SdcSchoolCollectionEndpoint {

    private static final SdcSchoolCollectionMapper mapper = SdcSchoolCollectionMapper.mapper;

    private final SdcSchoolCollectionService sdcSchoolCollectionService;

    private final SdcSchoolCollectionValidator sdcSchoolCollectionValidator;

    public SdcSchoolCollectionController(SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionValidator sdcSchoolCollectionValidator) {
        this.sdcSchoolCollectionService = sdcSchoolCollectionService;
      this.sdcSchoolCollectionValidator = sdcSchoolCollectionValidator;
    }

    @Override
    public SdcSchoolCollection getSchoolCollection(UUID sdcSchoolCollectionID) {
        return mapper.toSdcSchoolBatch(sdcSchoolCollectionService.getSdcSchoolCollection(sdcSchoolCollectionID));
    }

  @Override
  public SdcSchoolCollection updateSchoolCollection(SdcSchoolCollection sdcSchoolCollection, UUID sdcSchoolCollectionID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionValidator.validatePayload(sdcSchoolCollection, false));
    RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollection);
    return mapper.toSdcSchoolBatch(sdcSchoolCollectionService.updateSdcSchoolCollection(mapper.toSdcSchoolBatchEntity(sdcSchoolCollection)));
  }

  @Override
  public SdcSchoolCollection getActiveSchoolCollectionBySchoolId(UUID schoolID) {
    return mapper.toSdcSchoolBatch(
        sdcSchoolCollectionService.getActiveSdcSchoolCollectionBySchoolID(schoolID));
  }

  @Override
  public SdcSchoolCollection createSdcSchoolCollectionByCollectionID(SdcSchoolCollection sdcSchoolCollection, UUID collectionID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionValidator.validatePayload(sdcSchoolCollection, true));
    RequestUtil.setAuditColumnsForCreate(sdcSchoolCollection);
    return mapper.toStructure(sdcSchoolCollectionService.createSdcSchoolCollectionByCollectionID(mapper.toModel(sdcSchoolCollection), collectionID));
  }

  @Override
  public ResponseEntity<Void> deleteSdcSchoolCollection(UUID sdcSchoolCollectionID) {
      this.sdcSchoolCollectionService.deleteSdcCollection(sdcSchoolCollectionID);
      return ResponseEntity.noContent().build();
  }

    @Override
    public List<SdcSchoolCollection> getAllSchoolCollectionsBySchoolId(UUID schoolID) {
        List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = this.sdcSchoolCollectionService.getAllSchoolCollectionsBySchoolId(schoolID);

        List<SdcSchoolCollection> sdcSchoolCollectionList = new ArrayList<>();
        for(SdcSchoolCollectionEntity entity: sdcSchoolCollectionEntities){
            sdcSchoolCollectionList.add(mapper.toSdcSchoolBatch(entity));
        }
        return sdcSchoolCollectionList;
    }
}
