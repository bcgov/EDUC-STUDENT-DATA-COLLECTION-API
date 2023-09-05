package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

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
public class HeadCountTableDataRow implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private List<String> columnHeaderOrders;
    private Map<String, String> columnTitleAndValueMap;
}
