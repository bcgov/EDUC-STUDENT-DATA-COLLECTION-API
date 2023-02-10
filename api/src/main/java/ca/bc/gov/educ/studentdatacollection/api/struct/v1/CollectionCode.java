package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
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

  @NotNull(message = "collection code cannot be null")
  private String collectionCode;

  private String label;

  private String description;

  @NotNull(message = "openDate cannot be null")
  private String openDate;

  @NotNull(message = "closeDate cannot be null")
  private String closeDate;

  private Integer displayOrder;

  private String effectiveDate;

  private String expiryDate;

}
