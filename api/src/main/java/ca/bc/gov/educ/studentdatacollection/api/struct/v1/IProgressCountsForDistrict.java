package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import java.time.LocalDateTime;

public interface IProgressCountsForDistrict {

    byte[] getSdcSchoolCollectionID();
    LocalDateTime getUploadDate();
    String getUploadFileName();
    byte[] getSchoolID();
    long getTotalCount();
    long getLoadedCount();
    long getPosition();
}
