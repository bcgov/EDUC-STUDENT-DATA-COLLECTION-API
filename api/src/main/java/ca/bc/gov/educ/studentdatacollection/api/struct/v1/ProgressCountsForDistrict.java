package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProgressCountsForDistrict {

    UUID sdcSchoolCollectionID;
    LocalDateTime uploadDate;
    String uploadFileName;
    UUID schoolID;
    long totalCount;
    long loadedCount;
}
