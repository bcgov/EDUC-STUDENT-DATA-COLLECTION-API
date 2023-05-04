package ca.bc.gov.educ.studentdatacollection.api.rules.impl;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.rules.BaseRule;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PenRule implements BaseRule {

    private final ValidationRulesService validationRulesService;

    public PenRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    @Override
    public boolean shouldExecute(SdcStudentSagaData sdcStudentSagaData) {
        return CollectionTypeCodes.findByValue(sdcStudentSagaData.getCollectionTypeCode(), sdcStudentSagaData.getSchool().getSchoolCategoryCode()).isPresent();
    }

    @Override
    public List<SdcSchoolCollectionStudentValidationIssue> executeValidation(SdcStudentSagaData sdcStudentSagaData) {
        final List<SdcSchoolCollectionStudentValidationIssue> errors = new ArrayList<>();
        Long penCount = validationRulesService.getDuplicatePenCount(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionID(), sdcStudentSagaData.getSdcSchoolCollectionStudent().getStudentPen());
        if(StringUtils.isEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getStudentPen()) || penCount > 1) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.STUDENT_PEN, SdcSchoolCollectionStudentValidationIssueTypeCode.STUDENT_PEN_DUPLICATE));
        }

        if(StringUtils.isNotEmpty(sdcStudentSagaData.getSdcSchoolCollectionStudent().getStudentPen()) && !validCheckDigit(sdcStudentSagaData.getSdcSchoolCollectionStudent().getStudentPen())) {
            errors.add(createValidationIssue(SdcSchoolCollectionStudentValidationIssueSeverityCode.ERROR, SdcSchoolCollectionStudentValidationFieldCode.STUDENT_PEN, SdcSchoolCollectionStudentValidationIssueTypeCode.PEN_CHECK_DIGIT_ERR));
        }
        return errors;
    }

    protected boolean validCheckDigit(final String pen) {
        if (pen.length() != 9 || !pen.matches("-?\\d+(\\.\\d+)?")) {
            return false;
        }
        final List<Integer> odds = new ArrayList<>();
        final List<Integer> evens = new ArrayList<>();
        for (int i = 0; i < pen.length() - 1; i++) {
            final int number = Integer.parseInt(pen.substring(i, i + 1));
            if (i % 2 == 0) {
                odds.add(number);
            } else {
                evens.add(number);
            }
        }

        final int sumOdds = odds.stream().mapToInt(Integer::intValue).sum();

        final StringBuilder fullEvenStringBuilder = new StringBuilder();
        for (final int i : evens) {
            fullEvenStringBuilder.append(i);
        }

        final List<Integer> listOfFullEvenValueDoubled = new ArrayList<>();
        final String fullEvenValueDoubledString = Integer.toString(Integer.parseInt(fullEvenStringBuilder.toString()) * 2);
        for (int i = 0; i < fullEvenValueDoubledString.length(); i++) {
            listOfFullEvenValueDoubled.add(Integer.parseInt(fullEvenValueDoubledString.substring(i, i + 1)));
        }

        final int sumEvens = listOfFullEvenValueDoubled.stream().mapToInt(Integer::intValue).sum();

        final int finalSum = sumEvens + sumOdds;

        final String penCheckDigit = pen.substring(8, 9);

        return ((finalSum % 10 == 0 && penCheckDigit.equals("0")) || ((10 - finalSum % 10) == Integer.parseInt(penCheckDigit)));
    }
}
