package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SdcSchoolCollectionStudent extends BaseSdcSchoolStudent implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<SdcSchoolCollectionStudentValidationIssue> sdcSchoolCollectionStudentValidationIssues;

  private List<SdcSchoolCollectionStudentEnrolledProgram> sdcSchoolCollectionStudentEnrolledPrograms;

  private SdcStudentEll sdcStudentEll;

}
