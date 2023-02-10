package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

//TODO: Confirm I think this was supposed to be in the codebase and not a separate code table.

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "COLLECTION_TYPE_CODE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionTypeCodeEntity {

  @Id
  @Column(name = "COLLECTION_TYPE_CODE", unique = true, length = 10)
  private String collectionTypeCode;

  @Column(name = "LABEL", length = 30)
  private String label;

  @Column(name = "OPEN_DATE")
  private String openDate;

  @Column(name = "CLOSE_DATE")
  private String closeDate;

  @Column(name = "DESCRIPTION")
  private String description;

  @Column(name = "DISPLAY_ORDER")
  private Integer displayOrder;

  @Column(name = "EFFECTIVE_DATE")
  private String effectiveDate;

  @Column(name = "EXPIRY_DATE")
  private String expiryDate;

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
