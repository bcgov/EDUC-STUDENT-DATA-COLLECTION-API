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
public class BaseSDCSchoolStudent extends BaseRequest {

  private String sdcSchoolStudentID;

  @NotNull(message = "sdcSchoolBatchID cannot be null")
  private String sdcSchoolBatchID;

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
  @NotNull(message = "gender cannot be null")
  private String gender;

  @Size(max = 10)
  @NotNull(message = "specialEducationCategoryTypeCode cannot be null")
  private String specialEducationCategoryTypeCode;

  @Size(max = 10)
  @NotNull(message = "schoolFundingTypeCode cannot be null")
  private String schoolFundingTypeCode;

  @NotNull(message = "nativeIndianAncestryInd cannot be null")
  private Boolean nativeIndianAncestryInd;

  @Size(max = 10)
  @NotNull(message = "homeLanguageSpokenTypeCode cannot be null")
  private String homeLanguageSpokenTypeCode;

  private Integer otherCourses;

  private Integer supportBlocks;

  @Size(max = 10)
  @NotNull(message = "enrolledProgramTypeCode cannot be null")
  private String enrolledProgramTypeCode;

  @Size(max = 10)
  @NotNull(message = "careerProgramTypeCode cannot be null")
  private String careerProgramTypeCode;

  private Integer numberOfCourses;

  @Size(max = 4)
  private String bandTypeCode;

  @Size(max = 6)
  private String postalCode;

  @Size(max = 10)
  @NotNull(message = "statusTypeCode cannot be null")
  private String statusTypeCode;

}
