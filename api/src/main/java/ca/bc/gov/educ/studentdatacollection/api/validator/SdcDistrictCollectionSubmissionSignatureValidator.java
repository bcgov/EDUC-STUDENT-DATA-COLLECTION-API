package ca.bc.gov.educ.studentdatacollection.api.validator;


import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollectionSubmissionSignature;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class SdcDistrictCollectionSubmissionSignatureValidator {

    public static final String[] allowedRoles = new String[]{"DIS_SDC_EDIT", "SUPERINT", "SECR_TRES"};
    public static final String DISTRICT_SIGNATORY_ROLE = "districtSignatoryRole";

    public List<FieldError> validatePayload(SdcDistrictCollectionSubmissionSignature signature) {

        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if(Arrays.stream(allowedRoles).noneMatch(role -> role.equalsIgnoreCase(signature.getDistrictSignatoryRole()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError(DISTRICT_SIGNATORY_ROLE, signature.getDistrictSignatoryRole(), "Invalid District Signatory Role"));
        }

        return apiValidationErrors;
    }
}
