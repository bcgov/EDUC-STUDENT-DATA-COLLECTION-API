package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class CollectionCode extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @Size(max = 10)
  @NotNull(message = "collection code cannot be null")
  private String collectionCode;

  @Size(max = 30)
  @NotNull(message = "label cannot be null")
  private String label;

  @Size(max = 255)
  @NotNull(message = "description cannot be null")
  private String description;

  @NotNull(message = "openDate cannot be null")
  private String openDate;

  @NotNull(message = "closeDate cannot be null")
  private String closeDate;

  @NotNull(message = "displayOrder cannot be null")
  private Integer displayOrder;

  @NotNull(message = "effectiveDate cannot be null")
  private String effectiveDate;

  @NotNull(message = "expiryDate cannot be null")
  private String expiryDate;

}
