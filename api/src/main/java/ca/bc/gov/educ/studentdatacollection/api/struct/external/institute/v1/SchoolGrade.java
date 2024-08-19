package ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchoolGrade extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 6118916290604876032L;

  private String schoolGradeId;

  private String schoolId;

  @Size(max = 10)
  @NotNull(message = "schoolGradeCode cannot be null")
  private String schoolGradeCode;
}
