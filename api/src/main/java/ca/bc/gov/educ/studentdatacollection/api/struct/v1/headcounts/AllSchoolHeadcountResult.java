package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

import java.util.UUID;

public class AllSchoolHeadcountResult {

    private final UUID schoolID;
    private final Long totalHeadcount;

    public AllSchoolHeadcountResult(UUID schoolID, Long totalHeadcount) {
        this.schoolID = schoolID;
        this.totalHeadcount = totalHeadcount;
    }

    public UUID getSchoolID() {
        return schoolID;
    }

    public Long getTotalHeadcount() {
        return totalHeadcount;
    }
}


