package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static lombok.AccessLevel.PRIVATE;

@Service
public class ValidationRulesService {
    private final CodeTableService codeTableService;
    @Getter(PRIVATE)
    private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;
    private static final CodeTableMapper mapper = CodeTableMapper.mapper;
    public ValidationRulesService(CodeTableService codeTableService, SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository) {
        this.codeTableService = codeTableService;
        this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
    }
    public Long getDuplicatePenCount(String sdcSchoolID, String studentPen) {
        return sdcSchoolStudentRepository.countForDuplicateStudentPENs(UUID.fromString(sdcSchoolID), studentPen);
    }
    public List<EnrolledProgramCode> getActiveEnrolledProgramCodes() {
        return codeTableService.getAllEnrolledProgramCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<CareerProgramCode> getActiveCareerProgramCodes() {
        return codeTableService.getAllCareerProgramCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }
    public List<HomeLanguageSpokenCode> getActiveHomeLanguageSpokenCodes() {
        return codeTableService.getAllHomeLanguageSpokenCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }
    public  List<BandCode> getActiveBandCodes() {
        return codeTableService.getAllBandCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public  List<SchoolFundingCode> getActiveFundingCodes() {
        return codeTableService.getAllFundingCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<EnrolledGradeCode> getActiveGradeCodes() {
        return codeTableService.getAllGradeCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public List<SpecialEducationCategoryCode> getActiveSpecialEducationCategoryCodes() {
        return codeTableService.getAllSpecialEducationCategoryCodes().stream().filter(code -> code.getExpiryDate().isAfter(LocalDateTime.now())).map(mapper::toStructure).toList();
    }

    public boolean isEnrolledProgramCodeInvalid(String enrolledProgramCode) {
        final List<String> enrolledProgramCodes = splitString(enrolledProgramCode);
        return getActiveEnrolledProgramCodes().stream().noneMatch(programs -> enrolledProgramCodes.contains(programs.getEnrolledProgramCode()));
    }

    public List<String> splitString(String enrolledProgramCode) {
        return Pattern.compile(".{1,2}").matcher(enrolledProgramCode).results().map(MatchResult::group).toList();
    }
}
