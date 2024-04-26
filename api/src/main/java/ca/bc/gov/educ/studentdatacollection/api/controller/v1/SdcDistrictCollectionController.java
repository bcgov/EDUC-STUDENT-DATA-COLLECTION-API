package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDistrictCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDistrictCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.MonitorSdcSchoolCollectionsResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcInDistrictDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcDistrictCollectionValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SdcDistrictCollectionController implements SdcDistrictCollectionEndpoint {

  private static final SdcDistrictCollectionMapper mapper = SdcDistrictCollectionMapper.mapper;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final SdcDistrictCollectionService sdcDistrictCollectionService;
  private static final SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private final SdcDistrictCollectionValidator sdcDistrictCollectionValidator;

  public SdcDistrictCollectionController(SdcDuplicatesService sdcDuplicatesService, SdcDistrictCollectionService sdcDistrictCollectionService, SdcDistrictCollectionValidator sdcDistrictCollectionValidator) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.sdcDistrictCollectionService = sdcDistrictCollectionService;
      this.sdcDistrictCollectionValidator = sdcDistrictCollectionValidator;
  }

  @Override
  public SdcDistrictCollection getDistrictCollection(UUID sdcDistrictCollectionID) {
    return mapper.toStructure(sdcDistrictCollectionService.getSdcDistrictCollection(sdcDistrictCollectionID));
  }

  @Override
  public List<SdcInDistrictDuplicate> getDistrictCollectionDuplicates(UUID sdcDistrictCollectionID) {
    return this.sdcDuplicatesService.getAllInDistrictCollectionDuplicates(sdcDistrictCollectionID).stream().map(studentMapper::toSdcSchoolStudent).toList();
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
  public MonitorSdcSchoolCollectionsResponse getMonitorSdcSchoolCollectionResponse(UUID sdcDistrictCollectionId) {
    return this.sdcDistrictCollectionService.getMonitorSdcSchoolCollectionResponse(sdcDistrictCollectionId);
  }

  @Override
  public SdcDistrictCollection updateDistrictCollection(SdcDistrictCollection sdcDistrictCollection, UUID sdcDistrictCollectionID) {
    ValidationUtil.validatePayload(() -> this.sdcDistrictCollectionValidator.validatePayload(sdcDistrictCollection, false));
    RequestUtil.setAuditColumnsForUpdate(sdcDistrictCollection);
    return mapper.toStructure(sdcDistrictCollectionService.updateSdcDistrictCollection(mapper.toModel(sdcDistrictCollection)));
  }
}
