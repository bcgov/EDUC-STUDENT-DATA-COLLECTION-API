package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION_HISTORY")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionHistoryEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_COLLECTION_HISTORY_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcSchoolCollectionHistoryID;

  @Column(name = "SDC_SCHOOL_COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID sdcSchoolCollectionID;

  @Column(name = "COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID collectionID;

  @Column(name = "SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID schoolID;

  @Column(name = "DISTRICT_ID", columnDefinition = "BINARY(16)")
  private UUID districtID;

  @Column(name = "UPLOAD_DATE")
  private LocalDateTime uploadDate;

  @Column(name = "UPLOAD_FILE_NAME")
  private String uploadFileName;

  @Column(name = "UPLOAD_REPORT_DATE")
  private String uploadReportDate;

  @Column(name = "SDC_SCHOOL_COLLECTION_STATUS_CODE")
  private String sdcSchoolCollectionStatusCode;

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
