package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes.JULY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
class SummerRulesProcessorTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private RulesProcessor rulesProcessor;

    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    @Autowired
    RestUtils restUtils;
    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    @Test
    void testSummerGradePublicSchoolRuleIsExecuted() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledGradeCode("10");
        val saga = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_PUBLIC_SCHOOL_GRADE_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerGradePublicSchoolRuleIsNotExecuted_WhenFacilityTypeIsSUMMER() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledGradeCode("10");
        val saga = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_PUBLIC_SCHOOL_GRADE_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerAdultStudentRuleIsExecuted() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(22).format(format));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ADULT_STUDENT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerAdultStudentRuleIsNotExecuted_WhenStudentIsSchoolAged() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(19).format(format));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ADULT_STUDENT_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerFundingCodeRuleIsExecuted() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setSchoolFundingCode("14");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_FUNDING_CODE_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerPrePrimaryStudentIsExecuted() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(6).format(format));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_PRE_PRIMARY_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerEnrolledProgramAndSpedRuleIsExecuted_WhenSpedCodeIsA() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setSpecialEducationNonEligReasonCode("A");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ENROLLED_PROGRAM_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerEnrolledProgramAndSpedRuleIsExecuted_WhenEnrolledProgramCodeIsMalformed() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledProgramCodes("040");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ENROLLED_PROGRAM_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerFrenchAndCareerRuleIsExecuted_WhenEnrolledProgramCodeIs05() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledProgramCodes("05");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ENROLLED_PROGRAM_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerFrenchAndCareerRuleIsExecuted_WhenEnrolledProgramCodeIs40() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.SUMMER.getCode());
        collection.setCollectionTypeCode(JULY.getTypeCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledProgramCodes("40");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_ENROLLED_PROGRAM_ERROR.getCode()));
        assertThat(error).isTrue();
    }
}
