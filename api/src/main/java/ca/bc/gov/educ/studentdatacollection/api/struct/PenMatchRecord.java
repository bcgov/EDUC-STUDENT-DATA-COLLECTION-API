package ca.bc.gov.educ.studentdatacollection.api.struct;

import lombok.*;

/**
 * The type Pen match record.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PenMatchRecord {
  /**
   * The Matching pen.
   */
  private String matchingPEN;
  /**
   * The Student id.
   */
  private String studentID;
}
