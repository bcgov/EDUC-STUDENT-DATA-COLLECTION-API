package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.*;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SdcSchoolCollectionStudentHeadcounts implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<HeadcountHeader> headcountHeaders;
    private List<HeadcountTableData> headcountTableDataList;
}
