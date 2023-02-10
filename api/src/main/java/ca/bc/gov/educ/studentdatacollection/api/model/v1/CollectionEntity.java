package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "COLLECTION")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "COLLECTION_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID collectionID;

  @Column(name = "COLLECTION_CODE", nullable = false, length = 10)
  private String collectionCode;

  @Column(name = "OPEN_DATE")
  private String openDate;

  @Column(name = "CLOSE_DATE")
  private String closeDate;

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
  @OneToMany(mappedBy = "collectionEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = SdcSchoolEntity.class)
  Set<SdcSchoolEntity> sdcSchoolEntities;

  public Set<SdcSchoolEntity> getSDCSchoolEntities() {
    if (this.sdcSchoolEntities == null) {
      this.sdcSchoolEntities = new HashSet<>();
    }
    return this.sdcSchoolEntities;
  }

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "collectionEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = RtcSchoolEntity.class)
  Set<RtcSchoolEntity> rtcSchoolEntities;

  public Set<RtcSchoolEntity> getRTCSchoolEntities() {
    if (this.rtcSchoolEntities == null) {
      this.rtcSchoolEntities = new HashSet<>();
    }
    return this.rtcSchoolEntities;
  }

}
