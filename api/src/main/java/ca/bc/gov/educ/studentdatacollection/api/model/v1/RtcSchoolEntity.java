package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
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
@Table(name = "RECIPROCAL_TUITION_COLLECTION_SCHOOL")
@JsonIgnoreProperties(ignoreUnknown = true)
public class RtcSchoolEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "RTC_SCHOOL_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID rtcSchoolID;

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
  @OneToMany(mappedBy = "rtcSchoolEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = RtcSchoolStudentEntity.class)
  Set<RtcSchoolStudentEntity> rtcSchoolStudentEntities;

  public Set<RtcSchoolStudentEntity> getRTCSchoolStudentEntities() {
    if (this.rtcSchoolStudentEntities == null) {
      this.rtcSchoolStudentEntities = new HashSet<>();
    }
    return this.rtcSchoolStudentEntities;
  }

}
