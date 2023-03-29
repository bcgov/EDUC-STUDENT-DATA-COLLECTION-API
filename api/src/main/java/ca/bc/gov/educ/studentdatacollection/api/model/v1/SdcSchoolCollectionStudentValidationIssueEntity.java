package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionStudentValidationIssueEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_VALIDATION_ISSUE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sdcSchoolCollectionStudentValidationIssueID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionStudentEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID", updatable = false)
  SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity;

  @Column(name = "VALIDATION_ISSUE_SEVERITY_CODE", nullable = false)
  private String validationIssueSeverityCode;

  @Column(name = "VALIDATION_ISSUE_CODE", nullable = false)
  private String validationIssueCode;

  @Column(name = "VALIDATION_ISSUE_FIELD_CODE", nullable = false)
  private String validationIssueFieldCode;

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
