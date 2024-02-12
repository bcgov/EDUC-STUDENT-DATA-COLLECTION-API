package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class GradeEnrollementFTEReportGradesNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String code;

  private String schoolAgedHeadcount;

  private String schoolAgedEligibleForFTE;

  private String schoolAgedFTETotal;

  private String adultHeadcount;

  private String adultEligibleForFTE;

  private String adultFTETotal;

  private String allStudentHeadcount;

  private String allStudentEligibleForFTE;

  private String allStudentFTETotal;

}
