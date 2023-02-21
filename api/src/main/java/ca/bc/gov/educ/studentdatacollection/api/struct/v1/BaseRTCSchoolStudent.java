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
@EqualsAndHashCode(callSuper=false)
public class BaseRTCSchoolStudent extends BaseRequest {

  private String sdcSchoolStudentID;

  @NotNull(message = "rtcSchoolID cannot be null")
  private String rtcSchoolID;

  @Size(max = 12)
  private String localID;

  @Size(max = 10)
  @NotNull(message = "studentPen cannot be null")
  private String studentPen;

  @Size(max = 255)
  private String legalFirstName;

  @Size(max = 255)
  private String legalMiddleNames;

  @Size(max = 255)
  @NotNull(message = "legalLastName cannot be null")
  private String legalLastName;

  @Size(max = 255)
  private String usualFirstName;

  @Size(max = 255)
  private String usualMiddleNames;

  @Size(max = 255)
  private String usualLastName;

  @Size(max = 8)
  @NotNull(message = "dob cannot be null")
  private String dob;

  @Size(max = 1)
  @NotNull(message = "genderCode cannot be null")
  private String genderCode;

  @Size(max = 6)
  private String postalCode;

  @Size(max = 10)
  @NotNull(message = "statusCode cannot be null")
  private String statusCode;

}
