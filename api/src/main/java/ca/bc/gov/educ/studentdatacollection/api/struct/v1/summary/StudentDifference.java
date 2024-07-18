package ca.bc.gov.educ.studentdatacollection.api.struct.v1.summary;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class StudentDifference implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private SdcSchoolCollectionStudentHistory originalStudent;
  private SdcSchoolCollectionStudent currentStudent;

}
