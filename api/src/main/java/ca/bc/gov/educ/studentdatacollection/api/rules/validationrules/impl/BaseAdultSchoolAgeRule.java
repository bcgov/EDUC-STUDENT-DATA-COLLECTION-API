package ca.bc.gov.educ.studentdatacollection.api.rules.validationrules.impl;

import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;

public class BaseAdultSchoolAgeRule {

    protected final ValidationRulesService validationRulesService;

    public BaseAdultSchoolAgeRule(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    public void setupGraduateValues(StudentRuleData studentRuleData) {
        var student = studentRuleData.getSdcSchoolCollectionStudentEntity();
        if(student.getIsGraduated() == null){
            validationRulesService.updatePenMatchAndGradStatusColumns(student, studentRuleData.getSchool().getMincode());
        }
    }

}
