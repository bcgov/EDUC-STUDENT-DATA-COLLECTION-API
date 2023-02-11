package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "RECIPROCAL_TUITION_COLLECTION_STUDENT_VALIDATION_ISSUE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RtcStudentValidationIssueEntity {
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "RTC_STUDENT_VALIDATION_ISSUE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID rtcStudentValidationIssueId;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = RtcSchoolStudentEntity.class)
  @JoinColumn(name = "RTC_SCHOOL_STUDENT_ID", referencedColumnName = "RTC_SCHOOL_STUDENT_ID", updatable = false)
  private RtcSchoolStudentEntity rtcSchoolStudentEntity;

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
