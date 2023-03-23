package ca.bc.gov.educ.studentdatacollection.api.struct;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolStudent;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcStudentSagaData {
  private static final long serialVersionUID = -2329245910142215178L;
  private SdcSchoolStudent sdcSchoolStudent;
  private PenMatchResult penMatchResult;
}
