package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeadcountHeader implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private Map<String, HeadcountHeaderColumn> columns;
}
