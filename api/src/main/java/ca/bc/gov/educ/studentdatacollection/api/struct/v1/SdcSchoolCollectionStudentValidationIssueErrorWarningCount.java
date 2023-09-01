package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;
import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)

public class SdcSchoolCollectionStudentValidationIssueErrorWarningCount implements Serializable {

  @Serial
  private static final long serialVersionUID = 1L;

  private long error = 0;

  private long infoWarning = 0;

  private long fundingWarning = 0;

}
