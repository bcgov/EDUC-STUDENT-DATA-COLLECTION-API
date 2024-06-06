package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.time.LocalDateTime;
import java.util.UUID;

public interface MonitorIndySdcSchoolCollectionQueryResponse {

  UUID getSdcSchoolCollectionId();
  UUID getSchoolId();
  String getSdcSchoolCollectionStatusCode();
  LocalDateTime getUploadDate();
  String getUploadReportDate();
  long getHeadcount();
  long getErrors();
  long getFundingWarnings();
  long getInfoWarnings();
  long getUnresolvedProgramDuplicates();
  long getUnresolvedEnrollmentDuplicates();
}


