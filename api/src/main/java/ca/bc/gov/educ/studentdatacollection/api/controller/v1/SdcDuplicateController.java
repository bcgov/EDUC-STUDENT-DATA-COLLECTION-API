package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDuplicateEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicatesByInstituteID;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class SdcDuplicateController implements SdcDuplicateEndpoint {
  private final SdcDuplicatesService sdcDuplicatesService;
  private static final SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;
  public static final String INDY_SCHOOLS = "school";
  public static final String DISTRICTS = "district";

  public SdcDuplicateController(SdcDuplicatesService sdcDuplicatesService, SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.schoolCollectionStudentValidator = schoolCollectionStudentValidator;
      this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
  }

  @Override
  public SdcDuplicate updateStudentAndResolveDuplicates(String duplicateTypeResolutionCode, UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    sdcSchoolCollectionStudent.forEach(student -> ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(student)));
    if (DuplicateTypeResolutionCode.PROGRAM.getCode().equalsIgnoreCase(duplicateTypeResolutionCode)) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.updateStudentAndResolveProgramDuplicates(sdcDuplicateID, sdcSchoolCollectionStudent));
    } else if (DuplicateTypeResolutionCode.CHANGE_GRADE.getCode().equalsIgnoreCase(duplicateTypeResolutionCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.changeGrade(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    }
    return null;
  }

  @Override
  public ResponseEntity<Void> markPENForReview(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
    ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(sdcSchoolCollectionStudent));
    RequestUtil.setAuditColumnsForUpdate(sdcSchoolCollectionStudent);
    sdcSchoolCollectionStudentService.markPENForReview(studentMapper.toSdcSchoolStudentEntity(sdcSchoolCollectionStudent));
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @Override
  public Map<UUID, SdcDuplicatesByInstituteID> getInFlightProvincialDuplicates(UUID collectionID, String instituteType) {
    Map<UUID, SdcDuplicatesByInstituteID> duplicatesByInstituteIDMap;
    if(instituteType.equalsIgnoreCase(INDY_SCHOOLS)) {
      duplicatesByInstituteIDMap = sdcDuplicatesService.getInFlightProvincialDuplicates(collectionID, true);
    } else if(instituteType.equalsIgnoreCase(DISTRICTS)) {
      duplicatesByInstituteIDMap = sdcDuplicatesService.getInFlightProvincialDuplicates(collectionID, false);
    } else {
      log.error("Invalid type for getInFlightProvincialDuplicates::" + instituteType);
      throw new InvalidParameterException(instituteType);
    }
    return duplicatesByInstituteIDMap;
  }

  @Override
  public List<SdcDuplicate> getSchoolCollectionProvincialDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllProvincialDuplicatesBySdcSchoolCollectionID(sdcSchoolCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public List<SdcSchoolCollectionStudent> getSchoolCollectionDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllSchoolCollectionDuplicates(sdcSchoolCollectionID).stream().map(studentMapper::toSdcSchoolStudent).toList();
  }

  @Override
  public List<SdcDuplicate> getSchoolCollectionSdcDuplicates(UUID sdcSchoolCollectionID) {
    return this.sdcDuplicatesService.getAllDuplicatesBySdcSchoolCollectionID(sdcSchoolCollectionID).stream().map(duplicateMapper::toSdcDuplicate).toList();
  }

  @Override
  public SdcDuplicate getDuplicateByID(UUID sdcDuplicateID) {
    return duplicateMapper.toSdcDuplicate(this.sdcDuplicatesService.getSdcDuplicate(sdcDuplicateID));
  }
}
