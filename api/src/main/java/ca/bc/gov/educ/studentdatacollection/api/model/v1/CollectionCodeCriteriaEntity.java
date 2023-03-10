package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import java.time.LocalDateTime;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "COLLECTION_CODE_CRITERIA")
public class CollectionCodeCriteriaEntity {


  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "COLLECTION_CODE_CRITERIA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID collectionCodeCriteriaID;

  @ToString.Exclude
  @EqualsAndHashCode.Exclude
  @ManyToOne(optional = false, targetEntity = CollectionCodeEntity.class)
  @JoinColumn(name = "COLLECTION_CODE", referencedColumnName = "COLLECTION_CODE", updatable = false)
  CollectionCodeEntity collectionCodeEntity;

  @Column(name = "SCHOOL_CATEGORY_CODE")
  private String schoolCategoryCode;

  @Column(name = "FACILITY_TYPE_CODE")
  private String facilityTypeCode;

  @Column(name = "REPORTING_REQUIREMENT_CODE")
  private String reportingRequirementCode;

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
