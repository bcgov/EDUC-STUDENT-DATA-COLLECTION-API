package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

  private String rtcSchoolID;

  private String localID;

  private String studentPen;

  private String legalFirstName;

  private String legalMiddleNames;

  private String legalLastName;

  private String usualFirstName;

  private String usualMiddleNames;

  private String usualLastName;

  private String dob;

  private String genderCode;

  private String postalCode;

  private String statusCode;

}
