package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class MonitorSdcSchoolCollection implements Serializable {
  private static final long serialVersionUID = 1L;

  LocalDateTime uploadDate;
  UUID sdcSchoolCollectionId;
  String schoolTitle;
  long errors;
  long fundingWarnings;
  long infoWarnings;
  String schoolStatus;
  boolean submittedToDistrict;
}