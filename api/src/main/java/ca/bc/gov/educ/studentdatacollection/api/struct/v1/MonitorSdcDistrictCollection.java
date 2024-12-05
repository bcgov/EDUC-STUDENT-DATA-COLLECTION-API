package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class MonitorSdcDistrictCollection implements Serializable {
  private static final long serialVersionUID = 1L;

  UUID districtID;
  UUID sdcDistrictCollectionId;
  String districtTitle;
  String sdcDistrictCollectionStatusCode;
  String numSubmittedSchools;
  long unresolvedDuplicates;
}
