package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SdcStudentEll extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull(message = "sdcStudentEllID cannot be null")
  private String sdcStudentEllID;

  @NotNull(message = "studentID cannot be null")
  private String studentID;

  @NotNull(message = "Years in ELL cannot be null")
  private String yearsInEll;

}
