package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "STUDENT_DATA_COLLECTION_SCHOOL_STUDENT_HISTORY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolStudentHistoryEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_STUDENT_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sdcSchoolStudentHistoryID;

  @Basic
  @Column(name = "SDC_SCHOOL_STUDENT_ID", columnDefinition = "BINARY(16)")
  private UUID sdcSchoolStudentID;

  @Basic
  @Column(name = "SDC_SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID sdcSchoolID;

  @Column(name = "LOCAL_ID")
  private String localID;

  @Column(name = "STUDENT_PEN")
  private String studentPen;

  @Column(name = "LEGAL_FIRST_NAME")
  private String legalFirstName;

  @Column(name = "LEGAL_MIDDLE_NAMES")
  private String legalMiddleNames;

  @Column(name = "LEGAL_LAST_NAME")
  private String legalLastName;

  @Column(name = "USUAL_FIRST_NAME")
  private String usualFirstName;

  @Column(name = "USUAL_MIDDLE_NAMES")
  private String usualMiddleNames;

  @Column(name = "USUAL_LAST_NAME")
  private String usualLastName;

  @Column(name = "DOB")
  private String dob;

  @Column(name = "GENDER_TYPE_CODE", length = 1)
  private String genderTypeCode;

  @Column(name = "SPECIAL_EDUCATION_CATEGORY_TYPE_CODE")
  private String specialEducationCategoryTypeCode;

  @Column(name = "SCHOOL_FUNDING_TYPE_CODE")
  private String schoolFundingTypeCode;

  @Column(name = "NATIVE_INDIAN_ANCESTRY_IND")
  private Boolean nativeIndianAncestryInd;

  @Column(name = "HOME_LANGUAGE_SPOKEN_TYPE_CODE")
  private String homeLanguageSpokenTypeCode;

  @Column(name = "ENROLLED_PROGRAM_TYPE_CODE")
  private String enrolledProgramTypeCode;

  @Column(name = "CAREER_PROGRAM_TYPE_CODE")
  private String careerProgramTypeCode;

  @Column(name = "NUMBER_OF_COURSES")
  private Integer numberOfCourses;

  @Column(name = "BAND_TYPE_CODE")
  private String bandTypeCode;

  @Column(name = "POSTAL_CODE")
  private String postalCode;

  @Column(name = "STATUS_TYPE_CODE")
  private String statusTypeCode;

  @Column(name = "CREATE_USER", updatable = false , length = 32)
  private String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  private String updateUser;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  private LocalDateTime updateDate;

}
