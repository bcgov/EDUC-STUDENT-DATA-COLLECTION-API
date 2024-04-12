package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MonitorSdcSchoolCollectionQueryResponse {

  UUID getSdcSchoolCollectionId();
  UUID getSchoolId();
  String getSdcSchoolCollectionStatusCode();
  LocalDateTime getUploadDate();
  long getErrors();
  long getFundingWarnings();
  long getInfoWarnings();
}

