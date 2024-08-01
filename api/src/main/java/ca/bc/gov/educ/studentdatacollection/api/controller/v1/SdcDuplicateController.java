package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDuplicateEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
public class SdcDuplicateController implements SdcDuplicateEndpoint {
  private final SdcDuplicatesService sdcDuplicatesService;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;
  public static final String INDY_SCHOOLS = "school";
  public static final String DISTRICTS = "district";

  public SdcDuplicateController(SdcDuplicatesService sdcDuplicatesService, SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.schoolCollectionStudentValidator = schoolCollectionStudentValidator;
  }

  @Override
  public SdcDuplicate updateStudentAndResolveDuplicates(String duplicateTypeResolutionCode, UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    sdcSchoolCollectionStudent.forEach(student -> ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(student)));
    if (DuplicateTypeResolutionCode.PROGRAM.getCode().equalsIgnoreCase(duplicateTypeResolutionCode)) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.updateStudentAndResolveDuplicates(sdcDuplicateID, sdcSchoolCollectionStudent));
    } else if (DuplicateTypeResolutionCode.DELETE_ENROLLMENT_DUPLICATE.getCode().equalsIgnoreCase(duplicateTypeResolutionCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.softDeleteEnrollmentDuplicate(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    } else if (DuplicateTypeResolutionCode.CHANGE_GRADE.getCode().equalsIgnoreCase(duplicateTypeResolutionCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.trickleGradeChangeDupeUpdates(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    }
    return null;
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
}
