package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
public class SchoolIDListRequest {
    private List<UUID> schoolIDs;
}