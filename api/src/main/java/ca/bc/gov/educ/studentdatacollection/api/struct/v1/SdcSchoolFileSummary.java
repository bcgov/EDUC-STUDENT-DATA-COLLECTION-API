package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.*;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SdcSchoolFileSummary extends SdcFileSummary {
    private UUID sdcSchoolCollectionID;
    private UUID schoolID;
    private String schoolDisplayName;

    @Builder(builderMethodName = "childSummaryBuilder")
    public SdcSchoolFileSummary(UUID sdcSchoolCollectionID, UUID schoolID, String schoolDisplayName, SdcFileSummary fileSummary) {
        super(fileSummary.getCounts(), fileSummary.getFileName(), fileSummary.getUploadDate(), fileSummary.getUploadReportDate(), fileSummary.getTotalStudents(), fileSummary.getTotalProcessed());
        this.sdcSchoolCollectionID = sdcSchoolCollectionID;
        this.schoolID = schoolID;
        this.schoolDisplayName = schoolDisplayName;
    }
}
