package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
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
public class SdcSchoolCollectionStudentHeadcounts implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<HeadcountHeader> headcountHeaders;
    private List<HeadcountResultsTable> headcountResultsTable;
}
