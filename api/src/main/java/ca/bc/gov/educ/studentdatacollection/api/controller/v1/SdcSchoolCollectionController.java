package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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

  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  private final SdcSchoolCollectionValidator sdcSchoolCollectionValidator;

  public SdcSchoolCollectionController(SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionValidator sdcSchoolCollectionValidator) {
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionValidator = sdcSchoolCollectionValidator;
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
  public List<SdcSchoolCollection> getAllSchoolCollectionsBySchoolId(UUID schoolID) {
    List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = this.sdcSchoolCollectionService.getAllSchoolCollectionsBySchoolId(schoolID);

    List<SdcSchoolCollection> sdcSchoolCollectionList = new ArrayList<>();
    for (SdcSchoolCollectionEntity entity : sdcSchoolCollectionEntities) {
      sdcSchoolCollectionList.add(mapper.toStructure(entity));
    }
    return sdcSchoolCollectionList;
  }
}
