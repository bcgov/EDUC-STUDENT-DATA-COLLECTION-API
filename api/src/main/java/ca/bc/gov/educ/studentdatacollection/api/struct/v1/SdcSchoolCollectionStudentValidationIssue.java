package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SdcSchoolCollectionStudentValidationIssue extends BaseStudentValidationIssue implements Serializable {
  private static final long serialVersionUID = 1L;

  private String sdcSchoolCollectionStudentValidationIssueID;

  @NotNull(message = "sdcSchoolCollectionStudentID cannot be null")
  private String sdcSchoolCollectionStudentID;
}
