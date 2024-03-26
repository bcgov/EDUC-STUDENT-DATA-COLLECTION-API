package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Builder
@Table(name = "COLLECTION")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "COLLECTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID collectionID;

  @Column(name = "COLLECTION_TYPE_CODE", nullable = false, length = 10)
  @UpperCase
  private String collectionTypeCode;

  @Column(name = "OPEN_DATE")
  private LocalDateTime openDate;

  @Column(name = "CLOSE_DATE")
  private LocalDateTime closeDate;

  @Column(name = "SNAPSHOT_DATE")
  private LocalDate snapshotDate;

  @Column(name = "SUBMISSION_DUE_DATE")
  private LocalDate submissionDueDate;

  @Column(name = "DUPLICATION_RESOLUTION_DUE_DATE")
  private LocalDate duplicationResolutionDueDate;

  @Column(name = "SIGN_OFF_DUE_DATE")
  private LocalDate signOffDueDate;

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
  @OneToMany(mappedBy = "collectionEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcSchoolCollectionEntity.class)
  Set<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities;

  public Set<SdcSchoolCollectionEntity> getSDCSchoolEntities() {
    if (this.sdcSchoolCollectionEntities == null) {
      this.sdcSchoolCollectionEntities = new HashSet<>();
    }
    return this.sdcSchoolCollectionEntities;
  }

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "collectionEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcDistrictCollectionEntity.class)
  Set<SdcDistrictCollectionEntity> sdcDistrictCollectionEntities;

}
