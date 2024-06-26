package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDistrictCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDistrictCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcDistrictCollectionValidator;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
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
  private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;

  public SdcDistrictCollectionController(SdcDuplicatesService sdcDuplicatesService, SdcDistrictCollectionService sdcDistrictCollectionService, SdcDistrictCollectionValidator sdcDistrictCollectionValidator, SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.sdcDistrictCollectionService = sdcDistrictCollectionService;
      this.sdcDistrictCollectionValidator = sdcDistrictCollectionValidator;
      this.schoolCollectionStudentValidator = schoolCollectionStudentValidator;
  }

  @Override
  public SdcDistrictCollection getDistrictCollection(UUID sdcDistrictCollectionID) {
    return mapper.toStructure(sdcDistrictCollectionService.getSdcDistrictCollection(sdcDistrictCollectionID));
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
  public SdcDuplicate updateStudentAndResolveDistrictDuplicates(String duplicateTypeCode, UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    sdcSchoolCollectionStudent.forEach(student -> ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(student)));
    if (DuplicateTypeResolutionCode.PROGRAM.getCode().equalsIgnoreCase(duplicateTypeCode)) {
      return duplicateMapper.toSdcDuplicate(sdcDistrictCollectionService.updateStudentAndResolveDistrictDuplicates(sdcDuplicateID, sdcSchoolCollectionStudent));
    } else if (DuplicateTypeResolutionCode.DELETE_ENROLLMENT_DUPLICATE.getCode().equalsIgnoreCase(duplicateTypeCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDistrictCollectionService.softDeleteEnrollmentDuplicate(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    } else if (DuplicateTypeResolutionCode.CHANGE_GRADE.getCode().equalsIgnoreCase(duplicateTypeCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDistrictCollectionService.changeGrade(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    }
    return null;
  }

  @Override
  public SdcDistrictCollection unsubmitDistrictCollection(UnsubmitSdcDistrictCollection unsubmitData) {
    return mapper.toStructure(sdcDistrictCollectionService.unsubmitDistrictCollection(unsubmitData));
  }
}
