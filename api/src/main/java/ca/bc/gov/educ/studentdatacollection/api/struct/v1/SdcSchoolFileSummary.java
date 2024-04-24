package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.*;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Getter
public class SdcSchoolFileSummary{
    private UUID sdcSchoolCollectionID;
    private UUID schoolID;
    private String schoolDisplayName;
    private String fileName;
    private String percentageStudentsProcessed;
}
