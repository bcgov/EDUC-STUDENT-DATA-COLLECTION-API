package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Optional;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes.JULY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
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
    void testSummerPrePrimaryStudentIsNotExecuted() {
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
        assertThat(error).isFalse();
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

        entity.setDob(LocalDateTime.now().minusYears(3).format(format));
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
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_FRENCH_CAREER_PROGRAM_ERROR.getCode()));
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
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_FRENCH_CAREER_PROGRAM_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WhenStudentIsReportedInMAYCollectionInNotOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WhenStudentIsReportedInMAYCollectionInOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_CONT.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WhenStudentIsReportedInSEPTCollectionInNotOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse((LocalDate.now().getYear() - 1) + "-09-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WhenStudentIsReportedInSEPTCollectionInOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse((LocalDate.now().getYear() - 1) + "-09-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.DISTONLINE.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WithNoErrors_WhenStudentIsNotReportedInLastCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentReportedInOtherDistrictRuleIsExecuted_WhenStudentIsReportedInMAYCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);

        createHistoricalCollectionWithStudentInGrade07(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_NOT_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WithNoErrors_WhenStudentIsReportedInMAYCollectionInSameDistrictInOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WithNoErrors_WhenStudentInOnlineSchoolIsReportedInMAYCollectionInSameDistrictInStandardSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WhenStudentIsReportedInMAYCollectionInSameDistrictInStandardSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WithNoErrors_WhenStudentIsReportedInSEPTCollectionInDiffDistrictInOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-09-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);
        District district2 = createMockDistrict();
        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district2.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(1.25));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WhenStudentIsReportedInSEPTCollectionInDiffDistrictInNotOnlineSchool() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-09-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());
        createHistoricalCollectionWithStudent(CollectionTypeCodes.SEPTEMBER.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);
        District district2 = createMockDistrict();
        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district2.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WithNoErrors_WhenStudentInOnlineSchoolIsNotReportedInLastCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.DIST_LEARN.getCode());
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));

        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WithNoErrors_WhenStudentIsNotReportedInLastCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);
        doReturn(Optional.of(school)).when(restUtils).getSchoolBySchoolID(schoolId.toString());

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.getMatchingRecords().get(0).setStudentID(String.valueOf(assignedStudentID));
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(0));

        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isTrue();
    }


    private void createHistoricalCollectionWithStudent(String collectionTypeCode, LocalDateTime collectionCloseDate, UUID assignedStudentID, UUID districtID, UUID schoolID) {
        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(collectionTypeCode);
        collection.setCloseDate(collectionCloseDate);
        collection.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, districtID);
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolID);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("08");
        entity.setFte(BigDecimal.valueOf(1.00));
        sdcSchoolCollectionStudentRepository.save(entity);
    }

    @Test
    void testSummerStudentOnlineLearningRuleIsExecuted_WhenAssignedStudentIDIsNULL_And_StudentIsReportedInMAYCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);

        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus(null);
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(null);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_ONLINE_LEARNING_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    private void createHistoricalCollectionWithStudentInGrade07(String collectionTypeCode, LocalDateTime collectionCloseDate, UUID assignedStudentID) {
        District district = createMockDistrict();
        SchoolTombstone school = createMockSchool();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(collectionTypeCode);
        collection.setCloseDate(collectionCloseDate);
        collection.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setAssignedStudentId(assignedStudentID);
        entity.setEnrolledGradeCode("07");
        entity.setFte(BigDecimal.valueOf(1.00));
        sdcSchoolCollectionStudentRepository.save(entity);
    }

    @Test
    void testSummerStudentReportedInDistrictRuleIsExecuted_WhenAssignedStudentIDIsNULL_And_StudentIsReportedInMAYCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);

        createHistoricalCollectionWithStudent(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID, UUID.fromString(district.getDistrictId()), schoolId);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus(null);
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(null);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isFalse();
    }

    @Test
    void testSummerStudentReportedInOtherDistrictRuleIsExecuted_WhenAssignedStudentIDIsNULL_And_StudentIsReportedInMAYCollection() {
        UUID assignedStudentID = UUID.randomUUID();
        LocalDate mayCloseDate = LocalDate.parse(LocalDate.now().getYear() + "-05-30");
        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        LocalDateTime currentCloseDate = LocalDateTime.now().plusDays(2);

        createHistoricalCollectionWithStudentInGrade07(CollectionTypeCodes.MAY.getTypeCode(), LocalDateTime.of(mayCloseDate, LocalTime.MIDNIGHT), assignedStudentID);

        var collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(JULY.getTypeCode());
        collection.setCloseDate(currentCloseDate);
        collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollection);

        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, schoolId);
        sdcSchoolCollectionEntity.setSdcDistrictCollectionID(sdcDistrictCollection.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcSchoolCollectionEntity);

        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus(null);
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setAssignedStudentId(null);
        entity.setEnrolledGradeCode("08");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SUMMER_STUDENT_REPORTED_NOT_IN_DISTRICT_ERROR.getCode()));
        assertThat(error).isFalse();
    }
}
