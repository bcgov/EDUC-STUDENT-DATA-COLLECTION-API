package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcSchoolCollectionStudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.CodeTableAPIEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class CodeTableAPIController implements CodeTableAPIEndpoint {
    private final CodeTableService codeTableService;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;

    public CodeTableAPIController(CodeTableService codeTableService) {
        this.codeTableService = codeTableService;
    }

    @Override
    public List<EnrolledProgramCode> getEnrolledProgramCodes() {
        return codeTableService.getAllEnrolledProgramCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<CareerProgramCode> getCareerProgramCodes() {
        return codeTableService.getAllCareerProgramCodes().stream().map(mapper::toStructure).toList();
    }
    @Override
    public List<HomeLanguageSpokenCode> getHomeLanguageSpokenCodes() {
        return codeTableService.getAllHomeLanguageSpokenCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<BandCode> getBandCodes() {
        return codeTableService.getAllBandCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<SchoolFundingCode> getFundingCodes() {
        return codeTableService.getAllFundingCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<EnrolledGradeCode> getEnrolledGradeCodes() {
        return codeTableService.getAllEnrolledGradeCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<SpecialEducationCategoryCode> getSpecialEducationCategoryCodes() {
        return codeTableService.getAllSpecialEducationCategoryCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<GenderCode> getGenderCodes() {
        return codeTableService.getAllGenderCodes().stream().map(mapper::toStructure).toList();
    }

    @Override
    public List<ValidationIssueTypeCode> getValidationIssueTypeCodes() {
        List<ValidationIssueTypeCode> validationIssues = new ArrayList<>();

        for (var code : SdcSchoolCollectionStudentValidationIssueTypeCode.values()) {
            ValidationIssueTypeCode issue = new ValidationIssueTypeCode();
            issue.setValidationIssueTypeCode(code.getCode());
            issue.setMessage(code.getMessage());

            validationIssues.add(issue);
        }

        return  validationIssues;
    }

    @Override
    public List<SchoolFundingGroupCode> getIndependentSchoolFundingGroupCodes() {
        return codeTableService.getAllSchoolFundingGroupCodes().stream().map(mapper::toStructure).toList();
    }
}
