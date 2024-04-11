package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class MonitorSdcSchoolCollectionsResponse implements Serializable {
  private static final long serialVersionUID = 1L;

   List<MonitorSdcSchoolCollection> monitorSdcSchoolCollections;
   long schoolsWithData;
   long totalErrors;
   long totalFundingWarnings;
   long totalInfoWarnings;
   long schoolsDetailsConfirmed;
   long schoolsContactsConfirmed;
   long schoolsSubmitted;
   long totalSchools;
}