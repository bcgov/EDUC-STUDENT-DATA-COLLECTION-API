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
import java.util.*;

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

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = true, targetEntity = SdcSchoolCollectionStudentEntity.class)
  @JoinColumn(name = "RETAINED_SDC_SCHOOL_COLLECTION_STUDENT_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_STUDENT_ID")
  SdcSchoolCollectionStudentEntity retainedSdcSchoolCollectionStudentEntity;

  @Column(name = "DUPLICATE_SEVERITY_CODE")
  private String duplicateSeverityCode;

  @Column(name = "DUPLICATE_TYPE_CODE")
  private String duplicateTypeCode;

  @Column(name = "PROGRAM_DUPLICATE_TYPE_CODE")
  private String programDuplicateTypeCode;

  @Column(name = "DUPLICATE_LEVEL_CODE")
  private String duplicateLevelCode;

  @Column(name = "DUPLICATE_ERROR_DESCRIPTION_CODE")
  private String duplicateErrorDescriptionCode;

  @Column(name = "DUPLICATE_RESOLUTION_CODE")
  private String duplicateResolutionCode;

  @Column(name = "CREATE_USER", updatable = false , length = 32)
  private String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  private LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  private String updateUser;

  @Column(name="COLLECTION_ID")
  private UUID collectionID;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  private LocalDateTime updateDate;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @OneToMany(mappedBy = "sdcDuplicateEntity", fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = SdcDuplicateStudentEntity.class)
  Set<SdcDuplicateStudentEntity> sdcDuplicateStudentEntities;

  public Set<SdcDuplicateStudentEntity> getSdcDuplicateStudentEntities() {
    if (this.sdcDuplicateStudentEntities == null) {
      this.sdcDuplicateStudentEntities = new HashSet<>();
    }
    return this.sdcDuplicateStudentEntities;
  }

  public int getUniqueObjectHash() {
    List<UUID> studentIDs = getSdcDuplicateStudentEntities().stream().map(studentDupe -> studentDupe.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionStudentID()).toList();
    UUID smallerID = studentIDs.get(0).compareTo(studentIDs.get(1)) < 0 ? studentIDs.get(1) : studentIDs.get(0);
    UUID largerID = smallerID == studentIDs.get(0) ? studentIDs.get(1) : studentIDs.get(0);

    return Objects.hash(smallerID, largerID, duplicateSeverityCode, duplicateTypeCode, programDuplicateTypeCode, duplicateLevelCode,
            duplicateErrorDescriptionCode);
  }
}
