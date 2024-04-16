package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_COLLECTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcSchoolCollectionID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = CollectionEntity.class)
  @JoinColumn(name = "COLLECTION_ID", referencedColumnName = "COLLECTION_ID", updatable = false)
  CollectionEntity collectionEntity;

  @Basic
  @Column(name = "SCHOOL_ID", columnDefinition = "BINARY(16)")
  private UUID schoolID;

  @Column(name = "SDC_DISTRICT_COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID sdcDistrictCollectionID;

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

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "sdcSchoolCollection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = SdcSchoolCollectionStudentEntity.class)
  Set<SdcSchoolCollectionStudentEntity> sdcSchoolStudentEntities;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @OneToMany(mappedBy = "sdcSchoolCollection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcSchoolCollectionHistoryEntity.class)
  private Set<SdcSchoolCollectionHistoryEntity> sdcSchoolCollectionHistoryEntities;

  public Set<SdcSchoolCollectionStudentEntity> getSDCSchoolStudentEntities() {
    if (this.sdcSchoolStudentEntities == null) {
      this.sdcSchoolStudentEntities = new HashSet<>();
    }
    return this.sdcSchoolStudentEntities;
  }

  public Set<SdcSchoolCollectionHistoryEntity> getSdcSchoolCollectionHistoryEntities() {
    if (this.sdcSchoolCollectionHistoryEntities == null) {
      this.sdcSchoolCollectionHistoryEntities = new HashSet<>();
    }
    return this.sdcSchoolCollectionHistoryEntities;
  }

}
