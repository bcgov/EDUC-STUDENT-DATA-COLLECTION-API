package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AllSchoolHeadcountResult {
    UUID schoolID;
    Long totalHeadcount;
}
