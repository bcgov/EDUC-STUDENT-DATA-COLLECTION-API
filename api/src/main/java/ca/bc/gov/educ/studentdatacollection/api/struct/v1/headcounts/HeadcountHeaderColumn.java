package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeadcountHeaderColumn implements Serializable {
    private static final long serialVersionUID = 1L;
    private String currentValue;
    private String comparisonValue;
}
