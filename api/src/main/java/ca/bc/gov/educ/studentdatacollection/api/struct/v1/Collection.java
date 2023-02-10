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
public class Collection extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String collectionID;

  @NotNull(message = "collection code cannot be null")
  private String collectionCode;

  @NotNull(message = "open date cannot be null")
  private String openDate;

  @NotNull(message = "close date cannot be null")
  private String closeDate;

}
