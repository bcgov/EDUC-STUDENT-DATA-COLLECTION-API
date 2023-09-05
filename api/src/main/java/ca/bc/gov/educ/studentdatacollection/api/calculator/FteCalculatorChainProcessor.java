package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class FteCalculatorChainProcessor {

    private final List<FteCalculator> fteCalculators;

    @Autowired
    public FteCalculatorChainProcessor(final List<FteCalculator> fteCalculators) {
        this.fteCalculators = fteCalculators;
    }

    public FteCalculationResult processFteCalculator(StudentRuleData studentRuleData) {
        log.debug("Starting FTE calculation for student :: {} with data :: {}", studentRuleData.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID(), studentRuleData);
        for (int i = 0; i < fteCalculators.size() - 1; i++) {
            FteCalculator currentCalculator = fteCalculators.get(i);
            FteCalculator nextCalculator = fteCalculators.get(i + 1);
            currentCalculator.setNext(nextCalculator);
        }
        return fteCalculators.get(0).calculateFte(studentRuleData);
    }
}
