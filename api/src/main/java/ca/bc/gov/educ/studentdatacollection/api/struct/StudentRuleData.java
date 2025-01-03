package ca.bc.gov.educ.studentdatacollection.api.struct;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.UUID;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class StudentRuleData {
  private static final long serialVersionUID = -2329245910142215178L;
  private SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity;
  private SchoolTombstone school;
  private List<UUID> historicStudentIds;
  private boolean isMigratedStudent;
}
