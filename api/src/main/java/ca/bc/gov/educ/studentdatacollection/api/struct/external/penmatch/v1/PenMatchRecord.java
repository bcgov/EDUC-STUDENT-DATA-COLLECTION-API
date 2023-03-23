package ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1;

import lombok.*;

import java.io.Serializable;

/**
 * The type Pen match record.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenMatchRecord implements Serializable {
  private static final long serialVersionUID = 3445788842074331571L;
  /**
   * The Matching pen.
   */
  private String matchingPEN;
  /**
   * The Student id.
   */
  private String studentID;
}
