package ca.bc.gov.educ.studentdatacollection.api.struct;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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
public class StudentRuleData {
  private static final long serialVersionUID = -2329245910142215178L;
  private SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity;
  private String collectionTypeCode;
  private School school;
}
