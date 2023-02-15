package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import java.time.LocalDateTime;
import java.util.UUID;
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
@Table(name = "STUDENT_DATA_COLLECTION_SCHOOL_HISTORY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolHistoryEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcSchoolHistoryID;

  @Column(name = "COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID collectionID;

  @Column(name = "SDC_SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID sdcSchoolID;

  @Column(name = "SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID schoolID;

  @Column(name = "UPLOAD_DATE")
  private LocalDateTime uploadDate;

  @Column(name = "UPLOAD_FILE_NAME")
  private String uploadFileName;

  @Column(name = "COLLECTION_STATUS_TYPE_CODE")
  private String collectionStatusTypeCode;

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
