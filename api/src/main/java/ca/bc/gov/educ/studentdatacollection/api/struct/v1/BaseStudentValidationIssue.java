package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
@EqualsAndHashCode(callSuper = false)
public class BaseStudentValidationIssue extends BaseRequest {

  @Size(max = 10)
  @NotNull(message = "validationIssueSeverityCode cannot be null")
  private String validationIssueSeverityCode;

  @Size(max = 10)
  @NotNull(message = "validationIssueCode cannot be null")
  private String validationIssueCode;

  @Size(max = 10)
  @NotNull(message = "validationIssueFieldCode cannot be null")
  private String validationIssueFieldCode;
}
