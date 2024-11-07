package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDuplicateEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicateResolutionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class SdcDuplicateController implements SdcDuplicateEndpoint {
  private final SdcDuplicatesService sdcDuplicatesService;
  private final SdcDuplicateResolutionService sdcDuplicateResolutionService;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  private static final SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;

  public SdcDuplicateController(SdcDuplicatesService sdcDuplicatesService, SdcDuplicateResolutionService sdcDuplicateResolutionService, SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService) {
    this.sdcDuplicatesService = sdcDuplicatesService;
    this.sdcDuplicateResolutionService = sdcDuplicateResolutionService;
    this.schoolCollectionStudentValidator = schoolCollectionStudentValidator;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
  }

  @Override
  public SdcDuplicate updateStudentAndResolveDuplicates(String duplicateTypeResolutionCode, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudents) {
    sdcSchoolCollectionStudents.forEach(student -> ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(student)));
    if (DuplicateTypeResolutionCode.PROGRAM.getCode().equalsIgnoreCase(duplicateTypeResolutionCode)) {
      sdcDuplicateResolutionService.updateStudents(sdcSchoolCollectionStudents);
    } else if (DuplicateTypeResolutionCode.CHANGE_GRADE.getCode().equalsIgnoreCase(duplicateTypeResolutionCode) && sdcSchoolCollectionStudents.size() == 1) {
      sdcDuplicateResolutionService.changeGrade(studentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudents.get(0)));
    }
    return null;
  }

  @Override
  public List<SdcSchoolCollectionStudent> getSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllSchoolCollectionDuplicates(sdcSchoolCollectionID).stream().map(studentMapper::toSdcSchoolStudent).toList();
  }

  @Override
  public ResponseEntity<Void> markPENForReview(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudent));
    RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
    sdcSchoolCollectionStudentService.markPENForReview(studentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public List<SdcDuplicate> getSchoolCollectionProvincialDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllProvincialDuplicatesBySdcSchoolCollectionID(sdcSchoolCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public SdcDuplicate getDuplicateByID(UUID sdcDuplicateID) {
    return duplicateMapper.toSdcDuplicate(this.sdcDuplicatesService.getSdcDuplicate(sdcDuplicateID));
  }
}
