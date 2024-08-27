package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.util.PenUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@Component
public class SdcSchoolCollectionStudentValidator {

    public List<FieldError> validatePayload(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getStudentPen()) && !PenUtil.validCheckDigit(sdcSchoolCollectionStudent.getStudentPen())) {
            apiValidationErrors.add(ValidationUtil.createFieldError("studentPen", sdcSchoolCollectionStudent.getStudentPen(), "Invalid Student Pen."));
        }

        return apiValidationErrors;
    }
}
