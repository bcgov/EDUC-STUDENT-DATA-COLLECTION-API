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
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_DUPLICATE_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcDuplicateStudentEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_DUPLICATE_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcDuplicateStudentID;

  @Column(name = "SDC_DISTRICT_COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID sdcDistrictCollectionID;

  @Column(name = "SDC_SCHOOL_COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID sdcSchoolCollectionID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcDuplicateEntity.class)
  @JoinColumn(name = "SDC_DUPLICATE_ID", referencedColumnName = "SDC_DUPLICATE_ID", updatable = false)
  private SdcDuplicateEntity sdcDuplicateEntity;

  @ToString.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionStudentLightEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID", updatable = false)
  SdcSchoolCollectionStudentLightEntity sdcSchoolCollectionStudentEntity;

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
