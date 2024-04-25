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
@Table(name = "SDC_DUPLICATE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcDuplicateEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SDC_DUPLICATE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID sdcDuplicateID;

  @Column(name = "SDC_DISTRICT_COLLECTION_ID", columnDefinition = "BINARY(16)")
  private UUID sdcDistrictCollectionID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionStudentEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID_1", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID", updatable = false)
  SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent1Entity;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionStudentEntity.class)
  @JoinColumn(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID_2", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID", updatable = false)
  SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudent2Entity;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionStudentEntity.class)
  @JoinColumn(name = "RETAINED_SDC_SCHOOL_COLLECTION_STUDENT_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID", updatable = false)
  SdcSchoolCollectionStudentEntity retainedSdcSchoolCollectionStudentEntity;

  @Column(name = "DUPLICATE_SEVERITY_CODE")
  private String duplicateSeverityCode;

  @Column(name = "DUPLICATE_TYPE_CODE")
  private String duplicateTypeCode;

  @Column(name = "PROGRAM_DUPLICATE_TYPE_CODE")
  private String programDuplicateTypeCode;

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
