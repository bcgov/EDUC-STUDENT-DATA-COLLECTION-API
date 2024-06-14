package ca.bc.gov.educ.studentdatacollection.api.struct;

import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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
  private SdcSchoolCollectionStudent sdcSchoolCollectionStudent;
  private PenMatchResult penMatchResult;
  private String gradStatus;
  private String collectionTypeCode;
  private SchoolTombstone school;
}
