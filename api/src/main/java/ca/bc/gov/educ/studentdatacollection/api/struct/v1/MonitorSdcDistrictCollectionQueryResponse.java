package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.util.UUID;

public interface MonitorSdcDistrictCollectionQueryResponse {

  UUID getSdcDistrictCollectionID();
  UUID getDistrictID();
  String getSdcDistrictCollectionStatusCode();
  long getSubmittedSchools();
  long getTotalSchools();
  long getUnresolvedProgramDuplicates();
  long getUnresolvedEnrollmentDuplicates();
}

