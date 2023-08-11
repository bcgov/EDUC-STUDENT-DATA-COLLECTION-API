package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;

public interface FteCalculator {
    void setNext(FteCalculator nextCalculator);
    FteCalculationResult calculateFte(SdcStudentSagaData studentData);
}
