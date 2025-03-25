package ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.sdc;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SdcSchoolCollectionIdSchoolId {
    private UUID sdcSchoolCollectionID;
    private UUID schoolID;
}
