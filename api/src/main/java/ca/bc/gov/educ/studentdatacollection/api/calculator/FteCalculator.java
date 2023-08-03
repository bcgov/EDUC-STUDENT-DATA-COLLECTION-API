package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;

import java.util.Map;

public interface FteCalculator {
    void setNext(FteCalculator nextCalculator);
    Map<String, Object> calculateFte(SdcStudentSagaData studentData);
    int getProcessingSequenceNumber();
}
