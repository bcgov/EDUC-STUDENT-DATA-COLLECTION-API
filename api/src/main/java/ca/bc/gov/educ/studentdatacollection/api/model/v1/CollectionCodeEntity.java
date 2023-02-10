package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
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

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "COLLECTION_CODE")
public class CollectionCodeEntity {

  @Id
  @Column(name = "COLLECTION_CODE", unique = true, length = 10)
  private String collectionCode;

  /**
   * Display label for collection
   */
  @Column(name = "LABEL", length = 30)
  private String label;

  /**
   * Description for the collection code
   */
  @Column(name = "DESCRIPTION")
  private String description;

  /**
   * Display order of the collection types
   */
  @Column(name = "DISPLAY_ORDER")
  private Integer displayOrder;

  /**
   * When this collection code is effective
   */
  @Column(name = "EFFECTIVE_DATE")
  private LocalDateTime effectiveDate;

  /**
   * When this collection code expires
   */
  @Column(name = "EXPIRY_DATE")
  private LocalDateTime expiryDate;

  /**
   * Date the collection will open
   */
  @Column(name = "OPEN_DATE")
  private LocalDateTime openDate;

  /**
   * Date the collection will close
   */
  @Column(name = "CLOSE_DATE")
  private LocalDateTime closeDate;

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
  @OneToMany(mappedBy = "collectionCodeEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = CollectionCodeCriteriaEntity.class)
  Set<CollectionCodeCriteriaEntity> collectionCodeCriteriaEntities;

  public Set<CollectionCodeCriteriaEntity> getCollectionCodeCriteriaEntities() {
    if (this.collectionCodeCriteriaEntities == null) {
      this.collectionCodeCriteriaEntities = new HashSet<>();
    }
    return this.collectionCodeCriteriaEntities;
  }
}
