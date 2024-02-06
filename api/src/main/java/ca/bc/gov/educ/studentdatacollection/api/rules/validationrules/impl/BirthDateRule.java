package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculatorUtils;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.rules.ValidationBaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

/**
 *  | ID  | Severity | Rule                                       | Dependent On |
 *  |-----|----------|--------------------------------------------|--------------|
 *  | V04 | ERROR    | Birthdate must                             | NONE         |
 *                     be after Jan 1 1900 and
 *                     be a calendar date and
 *                     not be in the future
 *                     Birthdate cannot be blank.
 */
@Component
@Slf4j
@Order(40)
public class BirthDateRule implements ValidationBaseRule {

    @Override
    public boolean shouldExecute(StudentRuleData studentRuleData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        log.debug("In shouldExecute of BirthDateRule-V04: for collectionType {} and sdcSchoolCollectionStudentID :: {}" , FteCalculatorUtils.getCollectionTypeCode(studentRuleData),
                studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        return true;
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(StudentRuleData studentRuleData) {
        log.debug("In executeValidation of BirthDateRule-V04 for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
        if (StringUtils.isEmpty(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob())) {
            log.debug("BirthDateRule-V04: DOB is null for sdcSchoolCollectionStudentID ::" + studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
            errors.add(setValidationError());
        } else {
            try {
                LocalDate dob = LocalDate.parse(studentRuleData.getSdcSchoolCollectionStudentEntity().getDob(), format);
                LocalDate date = LocalDate.of(1900, Month.JANUARY, 02);
                if (dob.isAfter(LocalDate.now()) || dob.isBefore(date)) {
                    log.debug("BirthDateRule-V04: DOB - {} is either before 01-01-1900 or is in the future for sdcSchoolCollectionStudentID :: {}", dob, studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                    errors.add(setValidationError());
                }
            } catch (DateTimeParseException ex) {
                log.debug("BirthDateRule-V04: DOB - {} cannot be parsed:: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getDob(), studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID());
                errors.add(setValidationError());
            }
        }
        return errors;
    }

    private SdcSchoolCollectionStudentValidationIssue setValidationError() {
       return createValidationIssue(StudentValidationIssueSeverityCode.ERROR, StudentValidationFieldCode.DOB, StudentValidationIssueTypeCode.DOB_INVALID_FORMAT);
    }
}
