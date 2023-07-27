package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
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
@Order(40)
public class BirthDateRule implements BaseRule {

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData, List<SdcSchoolCollectionStudentValidationIssue> validationErrorsMap) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);
        if (StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob())) {
            errors.add(setValidationError());
        } else {
            try {
                LocalDate dob = LocalDate.parse(sdcStudentSagaData.getSdcSchoolCollectionStudent().getDob(), format);
                LocalDate date = LocalDate.of(1900, Month.JANUARY, 01);
                if (dob.isAfter(LocalDate.now()) || dob.isBefore(date)) {
                    errors.add(setValidationError());
                }
            } catch (DateTimeParseException ex) {
                errors.add(setValidationError());
            }
        }
        return errors;
    }

    private SdcSchoolCollectionStudentValidationIssue setValidationError() {
       return createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.DOB, SdcSchoolCollectionStudentValidationIssueTypeCode.DOB_INVALID_FORMAT);
    }
}
