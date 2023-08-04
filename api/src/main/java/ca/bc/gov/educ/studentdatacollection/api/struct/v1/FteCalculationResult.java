package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class FteCalculationResult {
    BigDecimal fte;
    String fteZeroReason;
}
