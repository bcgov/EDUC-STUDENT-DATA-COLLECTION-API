package ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * The type Pen match result.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class PenMatchResult implements Serializable {

  private static final long serialVersionUID = 7900220143043919913L;
  /**
   * The Matching records.
   */
  private List<PenMatchRecord> matchingRecords;
  /**
   * The Pen status.
   */
  private String penStatus;
  /**
   * The Pen status message.
   */
  private String penStatusMessage;
}
