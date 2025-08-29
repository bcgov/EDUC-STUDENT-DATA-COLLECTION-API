package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionStudentPaginationShallowEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sdcSchoolCollectionStudentID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_COLLECTION_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_ID", updatable = false)
  private SdcSchoolCollectionEntity sdcSchoolCollection;

  @Column(name = "LEGAL_FIRST_NAME")
  @UpperCase
  private String legalFirstName;

  @Column(name = "LEGAL_MIDDLE_NAMES")
  @UpperCase
  private String legalMiddleNames;

  @Column(name = "LEGAL_LAST_NAME")
  @UpperCase
  private String legalLastName;

  @Column(name = "USUAL_FIRST_NAME")
  @UpperCase
  private String usualFirstName;

  @Column(name = "USUAL_MIDDLE_NAMES")
  @UpperCase
  private String usualMiddleNames;

  @Column(name = "USUAL_LAST_NAME")
  @UpperCase
  private String usualLastName;

  @Column(name = "DOB")
  private String dob;

  @Column(name = "GENDER_CODE", length = 1)
  private String gender;

  @Column(name = "SPECIAL_EDUCATION_CATEGORY_CODE")
  private String specialEducationCategoryCode;

  @Column(name = "SCHOOL_FUNDING_CODE")
  private String schoolFundingCode;

  @Column(name = "NATIVE_ANCESTRY_IND")
  private String nativeAncestryInd;

  @Column(name = "ENROLLED_GRADE_CODE")
  private String enrolledGradeCode;

  @Column(name = "ENROLLED_PROGRAM_CODES")
  private String enrolledProgramCodes;

  @Column(name = "BAND_CODE")
  private String bandCode;

  @Column(name = "IS_ADULT")
  private Boolean isAdult;

  @Column(name = "IS_SCHOOL_AGED")
  private Boolean isSchoolAged;

  @Column(name = "FTE")
  private BigDecimal fte;

  @Column(name = "ASSIGNED_PEN")
  private String assignedPen;

  @Column(name = "ASSIGNED_STUDENT_ID", columnDefinition = "BINARY(16)")
  private UUID assignedStudentId;

}
