package ca.bc.gov.educ.studentdatacollection.api.endpoint.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping(URL.BASE_URL)
public interface CodeTableAPIEndpoint {

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.ENROLLED_PROGRAM_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "EnrolledProgramCode", implementation = EnrolledProgramCode.class)
    List<EnrolledProgramCode> getEnrolledProgramCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.CAREER_PROGRAM_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "CareerProgramCode", implementation = CareerProgramCode.class)
    List<CareerProgramCode> getCareerProgramCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.HOME_LANGUAGE_SPOKEN_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "HomeLanguageSpokenCode", implementation = HomeLanguageSpokenCode.class)
    List<HomeLanguageSpokenCode> getHomeLanguageSpokenCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.BAND_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "BandCode", implementation = BandCode.class)
    List<BandCode> getBandCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.FUNDING_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "SchoolFundingCode", implementation = SchoolFundingCode.class)
    List<SchoolFundingCode> getFundingCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.GRADE_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "EnrolledGradeCode", implementation = EnrolledGradeCode.class)
    List<EnrolledGradeCode> getEnrolledGradeCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.SPED_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "SpecialEducationCategoryCode", implementation = SpecialEducationCategoryCode.class)
    List<SpecialEducationCategoryCode> getSpecialEducationCategoryCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.GENDER_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "GenderCode", implementation = GenderCode.class)
    List<GenderCode> getGenderCodes();

    @PreAuthorize("hasAuthority('SCOPE_READ_COLLECTION_CODES')")
    @GetMapping(URL.VALIDATION_ISSUE_TYPE_CODES)
    @ApiResponses(value = {@ApiResponse(responseCode = "200", description = "OK")})
    @Transactional(readOnly = true)
    @Tag(name = "Collection Codes", description = "Endpoints to get collection codes.")
    @Schema(name = "ValidationIssueTypeCode", implementation = ValidationIssueTypeCode.class)
    List<ValidationIssueTypeCode> getValidationIssueTypeCodes();
}
