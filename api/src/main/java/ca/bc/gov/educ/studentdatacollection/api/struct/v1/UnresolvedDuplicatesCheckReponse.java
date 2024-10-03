package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serial;

@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class UnresolvedDuplicatesCheckReponse {
  @Serial
  private static final long serialVersionUID = 1L;

  boolean hasUnresolvedDuplicates;
}
