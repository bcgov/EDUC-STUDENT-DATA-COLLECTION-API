package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "STUDENT_DATA_COLLECTION_SCHOOL_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolStudentEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sdcSchoolStudentID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolBatchEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_ID", referencedColumnName = "SDC_SCHOOL_ID", updatable = false)
  SdcSchoolBatchEntity sdcSchoolBatchEntity;

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
  private String genderCode;

  @Column(name = "SPECIAL_EDUCATION_CATEGORY_TYPE_CODE")
  private String specialEducationCategoryCode;

  @Column(name = "SCHOOL_FUNDING_TYPE_CODE")
  private String schoolFundingCode;

  @Column(name = "NATIVE_INDIAN_ANCESTRY_IND")
  private Boolean nativeIndianAncestryInd;

  @Column(name = "HOME_LANGUAGE_SPOKEN_TYPE_CODE")
  private String homeLanguageSpokenTypeCode;

  @Column(name = "OTHER_COURSES")
  private String otherCourses;

  @Column(name = "SUPPORT_BLOCKS")
  private String supportBlocks;

  @Column(name = "ENROLLED_GRADE_TYPE_CODE")
  private String enrolledGradeCode;

  @Column(name = "ENROLLED_PROGRAM_TYPE_CODE")
  private String enrolledProgramCode;

  @Column(name = "CAREER_PROGRAM_TYPE_CODE")
  private String careerProgramCode;

  @Column(name = "NUMBER_OF_COURSES")
  private String numberOfCourses;

  @Column(name = "BAND_TYPE_CODE")
  private String bandTypeCode;

  @Column(name = "POSTAL_CODE")
  @UpperCase
  private String postalCode;

  @Column(name = "STATUS_CODE")
  private String statusCode;

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
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "sdcSchoolStudentEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcStudentValidationIssueEntity.class)
  Set<SdcStudentValidationIssueEntity> sdcStudentValidationIssueEntities;

  public Set<SdcStudentValidationIssueEntity> getSDCStudentValidationIssueEntities() {
    if (this.sdcStudentValidationIssueEntities == null) {
      this.sdcStudentValidationIssueEntities = new HashSet<>();
    }
    return this.sdcStudentValidationIssueEntities;
  }

}
