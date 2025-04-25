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
  private double totSeptLevel1s = 0;
  private double totSeptLevel2s = 0;
  private double totSeptLevel3s = 0;
  private double totSeptSES = 0;
  private double totFebLevel1s = 0;
  private double totFebLevel2s = 0;
  private double totFebLevel3s = 0;
  private double totFebSES = 0;
  private double totPositiveChangeLevel1s = 0;
  private double totPositiveChangeLevel2s = 0;
  private double totPositiveChangeLevel3s = 0;
  private double totPositiveChangeSES = 0;
  private double totNetLevel1s = 0;
  private double totNetLevel2s = 0;
  private double totNetLevel3s = 0;
  private double totNetSES = 0;
}
