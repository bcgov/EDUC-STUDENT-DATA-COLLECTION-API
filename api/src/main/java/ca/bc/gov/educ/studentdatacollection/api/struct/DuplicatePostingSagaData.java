package ca.bc.gov.educ.studentdatacollection.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DuplicatePostingSagaData {
  private static final long serialVersionUID = -2329245910142215178L;
  private UUID collectionID;
}
