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
@Table(name = "STUDENT_DATA_COLLECTION_SCHOOL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolBatchEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_SCHOOL_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcSchoolID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = CollectionEntity.class)
  @JoinColumn(name = "COLLECTION_ID", referencedColumnName = "COLLECTION_ID", updatable = false)
  CollectionEntity collectionEntity;

  @Basic
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

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "sdcSchoolEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcSchoolStudentEntity.class)
  Set<SdcSchoolStudentEntity> sdcSchoolStudentEntities;

  public Set<SdcSchoolStudentEntity> getSDCSchoolStudentEntities() {
    if (this.sdcSchoolStudentEntities == null) {
      this.sdcSchoolStudentEntities = new HashSet<>();
    }
    return this.sdcSchoolStudentEntities;
  }

}
