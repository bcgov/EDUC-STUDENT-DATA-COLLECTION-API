package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class ValidationIssueTypeCode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String ValidationIssueTypeCode;

  private String label;

  private String description;

  private Integer displayOrder;

  private String effectiveDate;

  private String expiryDate;

}
