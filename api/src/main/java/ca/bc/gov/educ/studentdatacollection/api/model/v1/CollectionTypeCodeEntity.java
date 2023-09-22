package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Builder
@Entity
@Table(name = "COLLECTION_TYPE_CODE")
public class CollectionTypeCodeEntity {

  @Id
  @Column(name = "COLLECTION_TYPE_CODE", unique = true, length = 10)
  private String collectionTypeCode;

  @Column(name = "LABEL", length = 30)
  private String label;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "DISPLAY_ORDER")
  private Integer displayOrder;

  @Column(name = "EFFECTIVE_DATE")
  private LocalDateTime effectiveDate;

  @Column(name = "EXPIRY_DATE")
  private LocalDateTime expiryDate;

  @Column(name = "OPEN_DATE")
  private LocalDateTime openDate;

  @Column(name = "CLOSE_DATE")
  private LocalDateTime closeDate;

  @Column(name = "SNAPSHOT_DATE")
  private LocalDateTime snapshotDate;

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
  @OneToMany(mappedBy = "collectionTypeCodeEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = CollectionCodeCriteriaEntity.class)
  Set<CollectionCodeCriteriaEntity> collectionCodeCriteriaEntities;

  public Set<CollectionCodeCriteriaEntity> getCollectionCodeCriteriaEntities() {
    if (this.collectionCodeCriteriaEntities == null) {
      this.collectionCodeCriteriaEntities = new HashSet<>();
    }
    return this.collectionCodeCriteriaEntities;
  }
}
