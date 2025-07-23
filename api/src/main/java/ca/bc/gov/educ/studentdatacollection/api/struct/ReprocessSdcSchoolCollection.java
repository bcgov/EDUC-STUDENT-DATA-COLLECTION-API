package ca.bc.gov.educ.studentdatacollection.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReprocessSdcSchoolCollection {
    @NotNull
    UUID sdcSchoolCollectionID;
    String updateUser;
}
