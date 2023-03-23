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
@Table(name = "ENROLLED_PROGRAM_CODE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class EnrolledProgramCodeEntity {
  @Id
  @Column(name = "ENROLLED_PROGRAM_CODE", unique = true, length = 10)
  String enrolledProgramCode;

  @Column(name = "LABEL", length = 30)
  String label;

  @Column(name = "DESCRIPTION")
  String description;

  @Column(name = "DISPLAY_ORDER")
  Integer displayOrder;

  @Column(name = "EFFECTIVE_DATE")
  String effectiveDate;

  @Column(name = "EXPIRY_DATE")
  String expiryDate;

  @Column(name = "CREATE_USER", updatable = false , length = 32)
  String createUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @Column(name = "UPDATE_USER", length = 32)
  String updateUser;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

}

