package ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * The type Pen match result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GradStatusResult implements Serializable {

  private static final long serialVersionUID = 7900220143043919913L;
  private String program;
  private String programCompletionDate;
  private String exception;
}
