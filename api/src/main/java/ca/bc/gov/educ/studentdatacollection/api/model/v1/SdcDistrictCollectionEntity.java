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
import java.util.Set;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_DISTRICT_COLLECTION")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcDistrictCollectionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
          @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_DISTRICT_COLLECTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcDistrictCollectionID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = CollectionEntity.class)
  @JoinColumn(name = "COLLECTION_ID", referencedColumnName = "COLLECTION_ID", updatable = false)
  CollectionEntity collectionEntity;

  @Basic
  @Column(name = "DISTRICT_ID", columnDefinition = "BINARY(16)")
  private UUID districtID;

  @Column(name = "SDC_DISTRICT_COLLECTION_STATUS_CODE")
  private String sdcDistrictCollectionStatusCode;

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
  @OneToMany(mappedBy = "sdcDistrictCollection", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcDistrictCollectionSubmissionSignatureEntity.class)
  Set<SdcDistrictCollectionSubmissionSignatureEntity> sdcDistrictCollectionSubmissionSignatureEntities;

}
