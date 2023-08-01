package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SchoolFundingGroupCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SchoolGradeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.IndependentSchoolFundingGroupService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@Component
public class IndependentSchoolFundingGroupPayloadValidator {

  public static final String COLLECTION_TYPE_CODE = "collectionTypeCode";

  @Getter(AccessLevel.PRIVATE)
  private final IndependentSchoolFundingGroupService independentSchoolFundingGroupService;

  @Getter(AccessLevel.PRIVATE)
  private final CodeTableService codeTableService;

  @Getter(AccessLevel.PRIVATE)
  private final RestUtils restUtils;

  @Autowired
  public IndependentSchoolFundingGroupPayloadValidator(final IndependentSchoolFundingGroupService independentSchoolFundingGroupService, final CodeTableService codeTableService, RestUtils restUtils) {
    this.independentSchoolFundingGroupService = independentSchoolFundingGroupService;
    this.codeTableService = codeTableService;
    this.restUtils = restUtils;
  }

  public List<FieldError> validatePayload(IndependentSchoolFundingGroup independentSchoolFundingGroup, boolean isCreateOperation) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    if (isCreateOperation && independentSchoolFundingGroup.getSchoolFundingGroupID() != null) {
      apiValidationErrors.add(ValidationUtil.createFieldError("schoolFundingGroupID", independentSchoolFundingGroup.getSchoolFundingGroupID(), "schoolFundingGroupID should be null for post operation."));
    }
    if (independentSchoolFundingGroup.getSchoolID() != null && !restUtils.getSchoolBySchoolID(independentSchoolFundingGroup.getSchoolID()).isPresent()) {
      apiValidationErrors.add(ValidationUtil.createFieldError("schoolID", independentSchoolFundingGroup.getSchoolID(), "schoolID was not found"));
    }
    validateCollectionCodePayload(independentSchoolFundingGroup, apiValidationErrors);

    return apiValidationErrors;
  }

  public List<FieldError> validateCreatePayload(IndependentSchoolFundingGroup independentSchoolFundingGroup) {
    return validatePayload(independentSchoolFundingGroup, true);
  }

  protected void validateCollectionCodePayload(IndependentSchoolFundingGroup independentSchoolFundingGroup, List<FieldError> apiValidationErrors) {
    List<SchoolFundingGroupCodeEntity> schoolFundingGroupCodes = codeTableService.getAllSchoolFundingGroupCodes();
    if (StringUtils.isNotEmpty(independentSchoolFundingGroup.getSchoolFundingGroupCode()) && schoolFundingGroupCodes.stream().noneMatch(fundingGroupCode -> fundingGroupCode.getSchoolFundingGroupCode().equals(independentSchoolFundingGroup.getSchoolFundingGroupCode()))) {
        apiValidationErrors.add(ValidationUtil.createFieldError("schoolFundingGroupCode", independentSchoolFundingGroup.getSchoolFundingGroupCode(), "Invalid School Funding Group Code."));
    }

    List<SchoolGradeCodeEntity> schoolGradeCodes = codeTableService.getAllSchoolGradeCodes();
    if (StringUtils.isNotEmpty(independentSchoolFundingGroup.getSchoolGradeCode()) && schoolGradeCodes.stream().noneMatch(gradeCode -> gradeCode.getSchoolGradeCode().equals(independentSchoolFundingGroup.getSchoolGradeCode()))) {
        apiValidationErrors.add(ValidationUtil.createFieldError("schoolGradeCode", independentSchoolFundingGroup.getSchoolGradeCode(), "Invalid School Grade Code."));
    }
  }

}
