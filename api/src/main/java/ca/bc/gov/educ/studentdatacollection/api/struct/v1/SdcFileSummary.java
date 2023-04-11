package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SdcFileSummary {
  private SdcFileSummaryCounts counts;
  private String fileName;
  private String uploadDate;
  private String totalStudents;
  private String totalProcessed;
}
