package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
public class SdcSchoolCollectionController implements SdcSchoolCollectionEndpoint {

  private static final SdcSchoolCollectionMapper mapper = SdcSchoolCollectionMapper.mapper;
  private static final SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private final SdcDuplicatesService sdcDuplicatesService;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;
  private final SdcSchoolCollectionValidator sdcSchoolCollectionValidator;

  public SdcSchoolCollectionController(SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionValidator sdcSchoolCollectionValidator, SdcDuplicatesService sdcDuplicatesService) {
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionValidator = sdcSchoolCollectionValidator;
    this.sdcDuplicatesService = sdcDuplicatesService;
  }

  @Override
  public SdcSchoolCollection getSchoolCollection(UUID sdcSchoolCollectionID) {
    return mapper.toStructure(sdcSchoolCollectionService.getSdcSchoolCollection(sdcSchoolCollectionID));
  }

  @Override
  public List<SdcSchoolCollectionStudent> getSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcSchoolCollectionService.getAllSchoolCollectionDuplicates(sdcSchoolCollectionID).stream().map(studentMapper::toSdcSchoolStudent).toList();
  }

  @Override
  public SdcDuplicate getDuplicateByID(UUID sdcDuplicateID) {
    return duplicateMapper.toSdcDuplicate(this.sdcDuplicatesService.getSdcDuplicate(sdcDuplicateID));
  }

  @Override
  public SdcSchoolCollection updateSchoolCollection(SdcSchoolCollection sdcSchoolCollection, UUID sdcSchoolCollectionID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionValidator.validatePayload(sdcSchoolCollection, false));
    RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollection);
    return mapper.toStructure(
        sdcSchoolCollectionService.updateSdcSchoolCollection(mapper.toSdcSchoolCollectionEntity(sdcSchoolCollection)));
  }

  @Override
  public SdcSchoolCollection getActiveSchoolCollectionBySchoolId(UUID schoolID) {
    return mapper.toStructure(sdcSchoolCollectionService.getActiveSdcSchoolCollectionBySchoolID(schoolID));
  }

  @Override
  public SdcSchoolCollection createSdcSchoolCollectionByCollectionID(SdcSchoolCollection sdcSchoolCollection, UUID collectionID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionValidator.validatePayload(sdcSchoolCollection, true));
    RequestUtil.setAuditColumnsForCreate(sdcSchoolCollection);
    SdcSchoolCollectionEntity entity = mapper.toModel(sdcSchoolCollection);
    if (!CollectionUtils.isEmpty(sdcSchoolCollection.getStudents())) {
      sdcSchoolCollection.getStudents().forEach(RequestUtil::setAuditColumnsForCreate);
      for (final var student : sdcSchoolCollection.getStudents()) {
        final var sdcStudentEntity = studentMapper.toSdcSchoolStudentEntity(student); sdcStudentEntity.setSdcSchoolCollection(entity);
        if (!CollectionUtils.isEmpty(student.getSdcSchoolCollectionStudentValidationIssues())) {
          sdcStudentEntity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(student.getSdcSchoolCollectionStudentValidationIssues(), sdcStudentEntity));
        }
        entity.getSDCSchoolStudentEntities().add(sdcStudentEntity);
      }
    }
    return mapper.toSdcSchoolWithStudents(sdcSchoolCollectionService.createSdcSchoolCollectionByCollectionID(entity, collectionID));
  }

  @Override
  public ResponseEntity<Void> deleteSdcSchoolCollection(UUID sdcSchoolCollectionID) {
    this.sdcSchoolCollectionService.deleteSdcCollection(sdcSchoolCollectionID);
    return ResponseEntity.noContent().build();
  }

  @Override
  public List<SdcSchoolCollection> getAllSchoolCollections(UUID schoolID, UUID sdcDistrictCollectionID) {
    List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = this.sdcSchoolCollectionService.getAllSchoolCollections(schoolID, sdcDistrictCollectionID);

    List<SdcSchoolCollection> sdcSchoolCollectionList = new ArrayList<>();
    for (SdcSchoolCollectionEntity entity : sdcSchoolCollectionEntities) {
      sdcSchoolCollectionList.add(mapper.toStructure(entity));
    }
    return sdcSchoolCollectionList;
  }

  @Override
  public SdcSchoolCollection unsubmitSchoolCollection(UnsubmitSdcSchoolCollection unsubmitData) {
    return mapper.toStructure(sdcSchoolCollectionService.unsubmitSchoolCollection(unsubmitData));
  }

  @Override
  public SdcSchoolCollection reportZeroEnrollment(ReportZeroEnrollmentSdcSchoolCollection reportZeroEnrollmentData) {
    return mapper.toStructure(sdcSchoolCollectionService.reportZeroEnrollment(reportZeroEnrollmentData));
  }

  @Override
  public List<SdcDuplicate> getSchoolCollectionProvincialDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllProvincialDuplicatesBySdcSchoolCollectionID(sdcSchoolCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }
}
