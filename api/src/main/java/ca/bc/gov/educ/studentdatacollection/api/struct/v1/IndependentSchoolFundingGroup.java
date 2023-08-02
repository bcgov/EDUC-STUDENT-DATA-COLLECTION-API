package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import jakarta.validation.constraints.NotNull;
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
public class IndependentSchoolFundingGroup extends BaseRequest implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String schoolFundingGroupID;

  @NotNull(message = "schoolID cannot be null")
  private String schoolID;

  @NotNull(message = "schoolGradeCode cannot be null")
  private String schoolGradeCode;

  @NotNull(message = "schoolFundingGroupCode cannot be null")
  private String schoolFundingGroupCode;

}
