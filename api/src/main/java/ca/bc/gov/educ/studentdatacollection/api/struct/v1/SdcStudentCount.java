package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Nominal roll student count.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SdcStudentCount {
  String status;
  long count;
}
