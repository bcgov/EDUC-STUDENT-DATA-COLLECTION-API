package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.util.DOBUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.PenUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class SdcSchoolCollectionStudentValidator {

    private final ValidationRulesService validationRulesService;

    public SdcSchoolCollectionStudentValidator(ValidationRulesService validationRulesService) {
        this.validationRulesService = validationRulesService;
    }

    public List<FieldError> validateUpdatePayload(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        return validatePayload(sdcSchoolCollectionStudent, false);
    }

    public List<FieldError> validateCreatePayload(SdcSchoolCollectionStudent sdcSchoolCollectionStudent) {
        return validatePayload(sdcSchoolCollectionStudent, true);
    }

    public List<FieldError> validatePayload(SdcSchoolCollectionStudent sdcSchoolCollectionStudent, boolean isCreate) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getStudentPen()) && !PenUtil.validCheckDigit(sdcSchoolCollectionStudent.getStudentPen())) {
            apiValidationErrors.add(ValidationUtil.createFieldError("studentPen", sdcSchoolCollectionStudent.getStudentPen(), "Invalid Student Pen."));
        }

        if (!DOBUtil.isValidDate(sdcSchoolCollectionStudent.getDob())) {
            apiValidationErrors.add(ValidationUtil.createFieldError("dob", sdcSchoolCollectionStudent.getDob(), "Invalid DOB."));
        }

        if (isCreate && sdcSchoolCollectionStudent.getSdcSchoolCollectionID() == null) {
            apiValidationErrors.add(ValidationUtil.createFieldError("sdcSchoolCollectionID", null, "sdcSchoolCollectionID cannot be null for create"));
        }

        List<SpecialEducationCategoryCode> activeSpecialEducationCategoryCode = validationRulesService.getActiveSpecialEducationCategoryCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getSpecialEducationCategoryCode()) && activeSpecialEducationCategoryCode.stream().noneMatch(program -> program.getSpecialEducationCategoryCode().equals(sdcSchoolCollectionStudent.getSpecialEducationCategoryCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("specialEducationCategoryCode", sdcSchoolCollectionStudent.getSpecialEducationCategoryCode(), "Invalid Special Education Category Code."));
        }

        List<SchoolFundingCode> activeFundingCodes = validationRulesService.getActiveFundingCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getSchoolFundingCode()) && activeFundingCodes.stream().noneMatch(code -> code.getSchoolFundingCode().equals(sdcSchoolCollectionStudent.getSchoolFundingCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("schoolFundingCode", sdcSchoolCollectionStudent.getSchoolFundingCode(), "Invalid School Funding Code."));
        }

        List<HomeLanguageSpokenCode> activeHomeLanguageCodes = validationRulesService.getActiveHomeLanguageSpokenCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getHomeLanguageSpokenCode()) && activeHomeLanguageCodes.stream().noneMatch(language -> language.getHomeLanguageSpokenCode().equals(sdcSchoolCollectionStudent.getHomeLanguageSpokenCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("homeLanguageSpokenCode", sdcSchoolCollectionStudent.getHomeLanguageSpokenCode(), "Invalid Home Language Spoken Code."));
        }

        List<EnrolledGradeCode> activeGradeCodes = validationRulesService.getActiveGradeCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getEnrolledGradeCode()) && activeGradeCodes.stream().noneMatch(code -> code.getEnrolledGradeCode().equals(sdcSchoolCollectionStudent.getEnrolledGradeCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("enrolledGradeCode", sdcSchoolCollectionStudent.getEnrolledGradeCode(), "Invalid Enrolled Grade Code."));
        }

        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getEnrolledProgramCodes()) && validationRulesService.isEnrolledProgramCodeInvalid(sdcSchoolCollectionStudent.getEnrolledProgramCodes())) {
            apiValidationErrors.add(ValidationUtil.createFieldError("enrolledProgramCodes", sdcSchoolCollectionStudent.getEnrolledProgramCodes(), "Invalid Enrolled Program Code."));
        }

        List<CareerProgramCode> activeCareerPrograms = validationRulesService.getActiveCareerProgramCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getCareerProgramCode()) && activeCareerPrograms.stream().noneMatch(programs -> programs.getCareerProgramCode().equals(sdcSchoolCollectionStudent.getCareerProgramCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("careerProgramCode", sdcSchoolCollectionStudent.getCareerProgramCode(), "Invalid Career Program Code."));
        }

        List<BandCode> activeBandCodes = validationRulesService.getActiveBandCodes();
        if (StringUtils.isNotEmpty(sdcSchoolCollectionStudent.getBandCode()) && activeBandCodes.stream().noneMatch(code -> code.getBandCode().equals(sdcSchoolCollectionStudent.getBandCode()))) {
            apiValidationErrors.add(ValidationUtil.createFieldError("bandCode", sdcSchoolCollectionStudent.getBandCode(), "Invalid Band Code."));
        }

        return apiValidationErrors;
    }
}
