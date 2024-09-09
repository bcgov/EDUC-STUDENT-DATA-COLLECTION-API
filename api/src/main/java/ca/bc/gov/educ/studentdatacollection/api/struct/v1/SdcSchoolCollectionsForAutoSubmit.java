package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

public interface SdcSchoolCollectionsForAutoSubmit {
  String getSdcSchoolCollectionID();
  long getErrorCount();
  long getDupeCount();
}
