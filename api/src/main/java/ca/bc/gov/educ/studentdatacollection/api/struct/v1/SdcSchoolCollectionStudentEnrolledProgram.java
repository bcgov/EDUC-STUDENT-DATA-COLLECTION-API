package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class SdcSchoolCollectionStudentEnrolledProgram extends BaseRequest implements Serializable {
  private static final long serialVersionUID = 1L;

  private String sdcSchoolCollectionStudentEnrolledProgramID;

  @NotNull(message = "sdcSchoolCollectionStudentID cannot be null")
  private String sdcSchoolCollectionStudentID;

  @NotNull(message = "enrolledProgramCode cannot be null")
  private String enrolledProgramCode;
}
