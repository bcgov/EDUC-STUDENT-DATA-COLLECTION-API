package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class SdcDistrictCollectionSubmissionSignature extends BaseRequest {
    private String sdcDistrictSubmissionSignatureID;

    @NotNull(message = "sdcDistrictCollection cannot be null")
    private String sdcDistrictCollectionID;

    @NotNull(message = "districtSignatoryUserID cannot be null")
    private String districtSignatoryUserID;

    @NotNull(message = "districtSignatoryRole cannot be null")
    private String districtSignatoryRole;
    private String signatureDate;

}
