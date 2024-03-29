package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeadcountHeader implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private HeadcountHeaderColumn headCountValue;
    private List<String> orderedColumnTitles;
    private Map<String, HeadcountHeaderColumn> columns;
}
