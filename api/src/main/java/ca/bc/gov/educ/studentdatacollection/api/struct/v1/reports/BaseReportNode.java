package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class BaseReportNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String collectionNameAndYear;

  private String reportGeneratedDate;

  private String districtNumberAndName;

  private String schoolMincodeAndName;

}
