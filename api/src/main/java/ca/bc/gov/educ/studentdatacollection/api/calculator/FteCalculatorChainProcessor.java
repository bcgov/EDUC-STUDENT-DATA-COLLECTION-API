package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Component
public class FteCalculatorChainProcessor {

    private final List<FteCalculator> fteCalculators;

    @Autowired
    public FteCalculatorChainProcessor(final List<FteCalculator> fteCalculators) {
        this.fteCalculators = fteCalculators;
    }

    public Map<String, Object> processFteCalculator(SdcStudentSagaData sdcStudentSagaData) {
        fteCalculators.sort(Comparator.comparing(FteCalculator::getProcessingSequenceNumber));

        for (int i = 0; i < fteCalculators.size() - 1; i++) {
            FteCalculator currentCalculator = fteCalculators.get(i);
            FteCalculator nextCalculator = fteCalculators.get(i + 1);
            currentCalculator.setNext(nextCalculator);
        }
        return fteCalculators.get(0).calculateFte(sdcStudentSagaData);
    }
}
