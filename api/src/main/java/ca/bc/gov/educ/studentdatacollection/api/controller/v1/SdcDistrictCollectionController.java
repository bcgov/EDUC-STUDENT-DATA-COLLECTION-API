package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDistrictCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionSubmissionSignatureMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDistrictCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcDistrictCollectionSubmissionSignatureValidator;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcDistrictCollectionValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class SdcDistrictCollectionController implements SdcDistrictCollectionEndpoint {

  private static final SdcDistrictCollectionMapper mapper = SdcDistrictCollectionMapper.mapper;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final SdcDistrictCollectionService sdcDistrictCollectionService;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private static final SdcSchoolCollectionMapper sdcSchoolCollectionMapper = SdcSchoolCollectionMapper.mapper;
  private final SdcDistrictCollectionValidator sdcDistrictCollectionValidator;
  private final SdcDistrictCollectionSubmissionSignatureValidator sdcDistrictCollectionSubmissionSignatureValidator;
    public SdcDistrictCollectionController(SdcDuplicatesService sdcDuplicatesService, SdcDistrictCollectionService sdcDistrictCollectionService, SdcDistrictCollectionValidator sdcDistrictCollectionValidator, SdcDistrictCollectionSubmissionSignatureValidator sdcDistrictCollectionSubmissionSignatureValidator) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.sdcDistrictCollectionService = sdcDistrictCollectionService;
      this.sdcDistrictCollectionValidator = sdcDistrictCollectionValidator;
      this.sdcDistrictCollectionSubmissionSignatureValidator = sdcDistrictCollectionSubmissionSignatureValidator;
  }

  @Override
  public SdcDistrictCollection getDistrictCollection(UUID sdcDistrictCollectionID) {
    return mapper.toStructureWithSubmissionSignatures(sdcDistrictCollectionService.getSdcDistrictCollection(sdcDistrictCollectionID));
  }

  @Override
  public List<SdcDuplicate> getDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    return this.sdcDuplicatesService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public List<SdcDuplicate> getDistrictCollectionProvincialDuplicates(UUID sdcDistrictCollectionID) {
    return this.sdcDuplicatesService.getAllProvincialDuplicatesBySdcDistrictCollectionID(sdcDistrictCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public SdcDistrictCollection getActiveDistrictCollectionByDistrictId(UUID districtID) {
    return mapper.toStructure(sdcDistrictCollectionService.getActiveSdcDistrictCollectionByDistrictID(districtID));
  }

  @Override
  public SdcDistrictCollection createSdcDistrictCollectionByCollectionID(SdcDistrictCollection sdcDistrictCollection, UUID collectionID) {
    ValidationUtil.validatePayload(() -> this.sdcDistrictCollectionValidator.validatePayload(sdcDistrictCollection, true));
    RequestUtil.setAuditColumnsForCreate(sdcDistrictCollection);
    SdcDistrictCollectionEntity entity = mapper.toModel(sdcDistrictCollection);
    return mapper.toStructure(sdcDistrictCollectionService.createSdcDistrictCollectionByCollectionID(entity, collectionID));
  }

  @Override
  public ResponseEntity<Void> deleteSdcDistrictCollection(UUID sdcDistrictCollectionID) {
    this.sdcDistrictCollectionService.deleteSdcDistrictCollection(sdcDistrictCollectionID);
    return ResponseEntity.noContent().build();
  }

  public List<SdcSchoolFileSummary> getSchoolCollectionsInProgress(UUID sdcDistrictCollectionID) {
    return sdcDistrictCollectionService.getSchoolCollectionsInProgress(sdcDistrictCollectionID);
  }

  @Override
  public List<SdcSchoolCollection> getSchoolCollectionsInDistrictCollection(UUID sdcDistrictCollectionID){
    List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = this.sdcDistrictCollectionService.getSchoolCollectionsInDistrictCollection(sdcDistrictCollectionID);

    List<SdcSchoolCollection> sdcSchoolCollectionList = new ArrayList<>();
    for (SdcSchoolCollectionEntity entity : sdcSchoolCollectionEntities) {
      sdcSchoolCollectionList.add(sdcSchoolCollectionMapper.toStructure(entity));
    }
    return sdcSchoolCollectionList;

  }

  @Override
  public MonitorSdcSchoolCollectionsResponse getMonitorSdcSchoolCollectionResponse(UUID sdcDistrictCollectionId) {
    return this.sdcDistrictCollectionService.getMonitorSdcSchoolCollectionResponse(sdcDistrictCollectionId);
  }

  @Override
  public SdcDistrictCollection updateDistrictCollection(SdcDistrictCollection sdcDistrictCollection, UUID sdcDistrictCollectionID) {
    ValidationUtil.validatePayload(() -> this.sdcDistrictCollectionValidator.validatePayload(sdcDistrictCollection, false));
    RequestUtil.setAuditColumnsForUpdate(sdcDistrictCollection);
    return mapper.toStructure(sdcDistrictCollectionService.updateSdcDistrictCollection(mapper.toModel(sdcDistrictCollection)));
  }

  @Override
  public SdcDistrictCollection unsubmitDistrictCollection(UnsubmitSdcDistrictCollection unsubmitData) {
    return mapper.toStructure(sdcDistrictCollectionService.unsubmitDistrictCollection(unsubmitData));
  }

  @Override
  public ResponseEntity<Void> signDistrictCollectionForSubmission(UUID sdcDistrictCollectionID, SdcDistrictCollection sdcDistrictCollection) {
    sdcDistrictCollection.getSubmissionSignatures().forEach(sign -> {
      ValidationUtil.validatePayload(() -> this.sdcDistrictCollectionSubmissionSignatureValidator.validatePayload(sign));
      if(sign.getSdcDistrictSubmissionSignatureID() == null) {
        RequestUtil.setAuditColumnsForCreateIfBlank(sign);
      }
    });
    sdcDistrictCollectionService.signDistrictCollectionForSubmission(sdcDistrictCollectionID, mapper.toModelWithSubmissionSignatures(sdcDistrictCollection));
    return ResponseEntity.ok().build();
  }
}
