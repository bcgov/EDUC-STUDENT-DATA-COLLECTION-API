package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

  private String schoolCategoryCode;

  private String facilityTypeCode;

  private String reportingRequirementCode;

}
