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
public class CollectionCodeCriteria extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String collectionCodeCriteriaID;

  @Size(max = 10)
  @NotNull(message = "collectionCode cannot be null")
  private String collectionCode;

  @Size(max = 10)
  private String schoolCategoryCode;

  @Size(max = 10)
  private String facilityTypeCode;

  @Size(max = 10)
  @NotNull(message = "reportingRequirementCode cannot be null")
  private String reportingRequirementCode;

}
