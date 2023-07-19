package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import java.math.BigDecimal;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class BaseSdcSchoolStudent extends BaseRequest {

  private String sdcSchoolCollectionStudentID;

  @NotNull(message = "sdcSchoolCollectionID cannot be null")
  private String sdcSchoolCollectionID;

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
  @NotNull(message = "specialEducationCategoryCode cannot be null")
  private String specialEducationCategoryCode;

  @Size(max = 10)
  @NotNull(message = "schoolFundingCode cannot be null")
  private String schoolFundingCode;

  @NotNull(message = "nativeAncestryInd cannot be null")
  private String nativeAncestryInd;

  @Size(max = 10)
  @NotNull(message = "homeLanguageSpokenCode cannot be null")
  private String homeLanguageSpokenCode;

  private String otherCourses;

  private String supportBlocks;

  @Size(max = 10)
  @NotNull(message = "enrolledGradeCode cannot be null")
  private String enrolledGradeCode;

  @Size(max = 10)
  @NotNull(message = "enrolledProgramCodes cannot be null")
  private String enrolledProgramCodes;

  @Size(max = 10)
  @NotNull(message = "careerProgramCode cannot be null")
  private String careerProgramCode;

  private String numberOfCourses;

  @Size(max = 4)
  private String bandCode;

  @Size(max = 6)
  private String postalCode;

  @Size(max = 10)
  @NotNull(message = "sdcSchoolCollectionStudentStatusCode cannot be null")
  private String sdcSchoolCollectionStudentStatusCode;

  private Boolean isAdult;

  private Boolean isSchoolAged;

  @DecimalMin(value = "0")
  @DecimalMax(value = "1")
  @Digits(integer = 1, fraction = 4)
  private BigDecimal fte;

  @Size(max = 10)
  private String fteZeroReasonCode;

  @Size(max = 10)
  private String frenchProgramNonEligReasonCode;

  @Size(max = 10)
  private String ellNonEligReasonCode;

  @Size(max = 10)
  private String indigenousSupportProgramNonEligReasonCode;

  @Size(max = 10)
  private String careerProgramNonEligReasonCode;

  @Size(max = 10)
  private String specialEducationNonEligReasonCode;

  private Boolean isGraduated;

}
