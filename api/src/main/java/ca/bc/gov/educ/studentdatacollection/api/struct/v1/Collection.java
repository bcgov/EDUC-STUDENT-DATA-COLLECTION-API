package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class Collection extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private String collectionID;

  @Size(max = 10)
  @NotNull(message = "collectionTypeCode cannot be null")
  private String collectionTypeCode;

  @NotNull(message = "open date cannot be null")
  private String openDate;

  @NotNull(message = "close date cannot be null")
  private String closeDate;

}
