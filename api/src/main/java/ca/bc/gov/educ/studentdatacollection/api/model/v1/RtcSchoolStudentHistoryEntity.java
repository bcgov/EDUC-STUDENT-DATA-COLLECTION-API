package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "RECIPROCAL_TUITION_COLLECTION_SCHOOL_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RtcSchoolStudentHistoryEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "RTC_SCHOOL_STUDENT_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID rtcSchoolStudentHistoryID;

  @Column(name = "rtcSchoolStudentID")
  private UUID rtcSchoolStudentID;

  @Column(name = "rtcSchoolID")
  private UUID rtcSchoolID;

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

  @Column(name = "POSTAL_CODE")
  @UpperCase
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
