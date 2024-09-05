package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SpedFundingReportTotals {
  private int totSeptLevel1s = 0;
  private int totSeptLevel2s = 0;
  private int totSeptLevel3s = 0;
  private int totFebLevel1s = 0;
  private int totFebLevel2s = 0;
  private int totFebLevel3s = 0;
  private int totPositiveChangeLevel1s = 0;
  private int totPositiveChangeLevel2s = 0;
  private int totPositiveChangeLevel3s = 0;
  private int totNetLevel1s = 0;
  private int totNetLevel2s = 0;
  private int totNetLevel3s = 0;
}
