package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DuplicateTypeResolutionCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDuplicateEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDuplicateMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDuplicatesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionStudentValidator;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class SdcDuplicateController implements SdcDuplicateEndpoint {
  private final SdcDuplicatesService sdcDuplicatesService;
  private static final SdcDuplicateMapper duplicateMapper = SdcDuplicateMapper.mapper;
  private final SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator;


  public SdcDuplicateController(SdcDuplicatesService sdcDuplicatesService, SdcSchoolCollectionStudentValidator schoolCollectionStudentValidator) {
      this.sdcDuplicatesService = sdcDuplicatesService;
      this.schoolCollectionStudentValidator = schoolCollectionStudentValidator;
  }

  @Override
  public SdcDuplicate updateStudentAndResolveDuplicates(String duplicateTypeCode, UUID sdcDuplicateID, List<SdcSchoolCollectionStudent> sdcSchoolCollectionStudent) {
    sdcSchoolCollectionStudent.forEach(student -> ValidationUtil.validatePayload(() -> this.schoolCollectionStudentValidator.validatePayload(student)));
    if (DuplicateTypeResolutionCode.PROGRAM.getCode().equalsIgnoreCase(duplicateTypeCode)) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.updateStudentAndResolveDuplicates(sdcDuplicateID, sdcSchoolCollectionStudent));
    } else if (DuplicateTypeResolutionCode.DELETE_ENROLLMENT_DUPLICATE.getCode().equalsIgnoreCase(duplicateTypeCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.softDeleteEnrollmentDuplicate(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    } else if (DuplicateTypeResolutionCode.CHANGE_GRADE.getCode().equalsIgnoreCase(duplicateTypeCode) && sdcSchoolCollectionStudent.size() == 1) {
      return duplicateMapper.toSdcDuplicate(sdcDuplicatesService.changeGrade(sdcDuplicateID, sdcSchoolCollectionStudent.get(0)));
    }
    return null;
  }
}
