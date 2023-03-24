package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "HOME_LANGUAGE_SPOKEN_CODE")
@JsonIgnoreProperties(ignoreUnknown = true)
@SuppressWarnings("squid:S1700")
public class HomeLanguageSpokenCode {
  @Id
  @Column(name = "HOME_LANGUAGE_SPOKEN_CODE", unique = true, length = 10)
  private String homeLanguageSpokenCode;

  @Column(name = "LABEL", length = 30)
  private String label;

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

