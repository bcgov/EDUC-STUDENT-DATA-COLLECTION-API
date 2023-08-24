package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class HeadcountTableData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String title;
    private List<String> columnNames;
    private List<HeadCountTableDataRow> rows;
}
