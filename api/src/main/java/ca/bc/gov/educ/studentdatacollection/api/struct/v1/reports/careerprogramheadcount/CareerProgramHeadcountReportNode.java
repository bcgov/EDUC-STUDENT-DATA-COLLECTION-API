package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.careerprogramheadcount;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.BaseChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.BaseReportNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class CareerProgramHeadcountReportNode extends BaseReportNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private List<BaseChildNode> careerPrograms;

}
