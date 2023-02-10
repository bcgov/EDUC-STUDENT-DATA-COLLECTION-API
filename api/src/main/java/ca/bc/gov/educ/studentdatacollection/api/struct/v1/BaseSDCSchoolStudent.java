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
public class BaseSDCSchoolStudent extends BaseRequest {

  private String sdcSchoolStudentID;

  private String sdcSchoolID;

  private String localID;

  private String studentPen;

  private String legalFirstName;

  private String legalMiddleNames;

  private String legalLastName;

  private String usualFirstName;

  private String usualMiddleNames;

  private String usualLastName;

  private String dob;

  private String genderTypeCode;

  private String specialEducationCategoryTypeCode;

  private String schoolFundingTypeCode;

  private Boolean nativeIndianAncestryInd;

  private String homeLanguageSpokenTypeCode;

  private String otherCourses;

  private String supportBlocks;

  private String enrolledProgramTypeCode;

  private String careerProgramTypeCode;

  private Integer numberOfCourses;

  private String bandTypeCode;

  private String postalCode;

  private String statusTypeCode;

}
