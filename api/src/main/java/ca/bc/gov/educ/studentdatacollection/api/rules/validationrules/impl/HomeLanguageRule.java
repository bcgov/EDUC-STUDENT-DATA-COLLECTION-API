package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HomeLanguageSpokenCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                                                  | Dependent On |
 *  |-----|----------|-----------------------------------------------------------------------|--------------|
 *  | V35 | ERROR    | If reported, a student's reported Language Spoken at Home must be     |  NONE     |
 *                     blank or appear in list of valid, non-expired home language codes.
 *
 */
@Component
@Slf4j
@Order(140)
public class HomeLanguageRule implements ValidationBaseRule {
    private final ValidationRulesService validationRulesService;

    public HomeLanguageRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of HomeLanguageRule-V35: for collectionType {} and sdcSchoolCollectionStudentID :: {}" + studentRuleData.getCollectionTypeCode(),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return CollectionTypeCodes.findByValue(studentRuleData.getCollectionTypeCode(), studentRuleData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of HomeLanguageRule-V35");
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        List<HomeLanguageSpokenCode> activeHomeLanguageCodes = validationRulesService.getActiveHomeLanguageSpokenCodes();
        if(StringUtils.isNotEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getHomeLanguageSpokenCode()) && activeHomeLanguageCodes.stream().noneMatch(language -> language.getHomeLanguageSpokenCode().equals(studentRuleData.getSdcSchoolCollectionStudentEntity().getHomeLanguageSpokenCode()))) {
            errors.add(createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.HOME_LANGUAGE_SPOKEN_CODE, StudentValidationIssueTypeCode.SPOKEN_LANG_ERR));
        }
        log.debug("HomeLanguageRule-V35 has errors::" + errors);
        return errors;
    }
}
