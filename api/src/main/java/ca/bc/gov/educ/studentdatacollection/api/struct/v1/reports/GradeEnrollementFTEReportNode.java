package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

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
@SuppressWarnings("squid:S1700")
public class GradeEnrollementFTEReportNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String collectionNameAndYear;

  private String reportGeneratedDate;

  private String districtNumberAndName;

  private String schoolMincodeAndName;

  private List<GradeEnrollementFTEReportGradesNode> grades;

}
