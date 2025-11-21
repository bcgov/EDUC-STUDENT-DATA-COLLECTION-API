package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SdcSchoolCollectionStudentHistoryPagination extends BaseSdcSchoolStudent implements Serializable {

  private static final long serialVersionUID = 1L;

  private String sdcSchoolCollectionStudentHistoryID;

  private String snapshotDate;

  private List<SdcSchoolCollectionStudentEnrolledProgram> sdcSchoolCollectionStudentEnrolledPrograms;

}

