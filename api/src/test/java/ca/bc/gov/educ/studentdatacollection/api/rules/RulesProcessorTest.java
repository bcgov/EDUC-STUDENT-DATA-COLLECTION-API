package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationFieldCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolFundingCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes.JULY;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes.SEPTEMBER;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes.DISTONLINE;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes.DIST_LEARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
class RulesProcessorTest extends BaseStudentDataCollectionAPITest {

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
    void testGenderRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        entity.setGender("M");
        val validationErrorM = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorM.size()).isZero();

        entity.setGender("F");
        val validationErrorF = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorF.size()).isZero();

        entity.setGender("U");
        val validationErrorIncorrectVal = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorIncorrectVal.size()).isNotZero();
        assertThat(validationErrorIncorrectVal.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

        entity.setGender(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorNull.size()).isNotZero();
        assertThat(validationErrorNull.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

        entity.setGender("");
        val validationErrorEmpty = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorEmpty.size()).isNotZero();
        assertThat(validationErrorEmpty.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

    }

    @Test
    void testDOBRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        entity.setDob("20230230");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("0210424F");
        val validationErrorInvalidCharDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInvalidCharDate.size()).isNotZero();
        assertThat(validationErrorInvalidCharDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob(format.format(LocalDate.now().plusDays(2)));
        val validationErrorFutureDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorFutureDate.size()).isNotZero();
        assertThat(validationErrorFutureDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("18990101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorOldDate.size()).isNotZero();
        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("");
        val validationErrorEmptyDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorEmptyDate.size()).isNotZero();
        assertThat(validationErrorEmptyDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20170420");
        val validationErrorCorrectDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCorrectDate.size()).isZero();
    }

    @Test
    void testLocalIDRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLocalID(null);
        val validationErrorBlank = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorBlank.size()).isNotZero();
        assertThat(validationErrorBlank.get(0).getValidationIssueFieldCode()).isEqualTo("LOCALID");
    }

    @Test
    void testStandardSchoolWithBlankPen() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("STANDARD");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isZero();

        entity.setStudentPen(null);
        val summerSagaData = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());
        val summerValidationErrorBlank = rulesProcessor.processRules(summerSagaData);
        assertThat(summerValidationErrorBlank.size()).isNotZero();

        val septemberSagaData = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(SEPTEMBER.getTypeCode());
        val septemberValidationErrorBlank = rulesProcessor.processRules(septemberSagaData);
        assertThat(septemberValidationErrorBlank.size()).isZero();
    }

    @Test
    void testSummerSchoolWithBlankPen(){
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isZero();

        entity.setStudentPen(null);
        val sagaData = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());
        val validationErrorBlank = rulesProcessor.processRules(sagaData);
        assertThat(validationErrorBlank.size()).isNotZero();
        assertThat(validationErrorBlank.get(0).getValidationIssueFieldCode()).isEqualTo("STUDENT_PEN");
        assertThat(validationErrorBlank.get(0).getValidationIssueCode()).isEqualTo("STUDENTPENBLANK");
    }

    @Test
    void testStudentLegalLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalLastName("Billie-Jean");
        val validationErrorNum = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorNum.size()).isZero();

        entity.setLegalLastName(null);
        val nullLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(nullLastNameErr.size()).isNotZero();
        assertThat(nullLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBLANK");

        entity.setLegalLastName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMECHARFIX");

        entity.setLegalLastName("'-.");
        val charsLastNameCharErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameCharErr.size()).isNotZero();
        assertThat(charsLastNameCharErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMECHARFIX");

        entity.setLegalLastName("FAKE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBADVALUE");
    }

    @Test
    void testStudentUsualLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualLastName("Bob$");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALLASTNAMECHARFIX");

        entity.setUsualLastName("FAKE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALLASTNAMEBADVALUE");

        entity.setUsualLastName(null);
        val nullLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(nullLastNameErr.size()).isZero();
    }

    @Test
    void testStudentLegalFirstNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalFirstName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALFIRSTNAMECHARFIX");

        entity.setLegalFirstName("DELETE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALFIRSTNAMEBADVALUE");
    }

    @Test
    void testStudentUsualFirstNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualFirstName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALFIRSTNAMECHARFIX");

        entity.setUsualFirstName("NOTAPPLICABLE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALFIRSTNAMEBADVALUE");
    }

    @Test
    void testStudentLegalMiddleNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalMiddleNames("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALMIDDLENAMECHARFIX");

        entity.setLegalMiddleNames("TeSTStuD");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALMIDDLENAMEBADVALUE");
    }

    @Test
    void testStudentUsualMiddleNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualMiddleNames("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALMIDDLENAMECHARFIX");

        entity.setUsualMiddleNames("na");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALMIDDLENAMEBADVALUE");
    }

    @Test
    void testCSFProgramRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("05");
        school.setSchoolReportingRequirementCode("RT");
        val validationCodeReportError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationCodeReportError.size()).isNotZero();
        val error1 = validationCodeReportError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDWRONGREPORTING"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes("4005");
        entity.setCareerProgramCode("XA");
        school.setSchoolReportingRequirementCode("CSF");
        val noValidationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(noValidationError.size()).isZero();
    }

    @Test
    void testEnrolledProgramParseRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("143");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo("ENROLLEDCODEPARSEERR");

        entity.setEnrolledProgramCodes("0 4017293633");
        val validationErrorSpace = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorSpace.size()).isNotZero();
        val error1 = validationErrorSpace.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEPARSEERR"));
        assertThat(error1).isTrue();
        val downstreamError = validationErrorSpace.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ENROLLED_CODE_INVALID.getCode()));
        assertThat(downstreamError).isFalse();
    }

    @Test
    void testDuplicateEnrolledProgramRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("4040");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo(StudentValidationIssueTypeCode.ENROLLED_CODE_DUP_ERR.getCode());
    }

    @Test
    void testDuplicateEnrolledProgramRuleDependencyOnV74() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("40407");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isNotEqualTo(StudentValidationIssueTypeCode.ENROLLED_CODE_DUP_ERR.getCode());
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo(StudentValidationIssueTypeCode.ENROLLED_CODE_PARSE_ERR.getCode());
    }


    @Test
    void testDuplicatePenRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        entity.setStudentPen("523456789");
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        sdcSchoolCollectionStudentRepository.save(entity);

        val savedEntityOne = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(savedEntityOne).isPresent();

        val entity2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity2.setStudentPen("523456789");
        entity2.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity2.setUpdateDate(LocalDateTime.now());
        entity2.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity2.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        sdcSchoolCollectionStudentRepository.save(entity2);

        val savedEntityTwo = sdcSchoolCollectionStudentRepository.findById(entity2.getSdcSchoolCollectionStudentID());
        assertThat(savedEntityTwo).isPresent();

        val dupePenCount = sdcSchoolCollectionStudentRepository.countForDuplicateStudentPENs(entity.getSdcSchoolCollection().getSdcSchoolCollectionID(), entity.getStudentPen());
        assertThat(dupePenCount).isEqualTo(2);

        entity.setStudentPen("523456789");
        val validationErrorDupe = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorDupe.size()).isNotZero();
        assertThat(validationErrorDupe.get(0).getValidationIssueCode()).isEqualTo("PENCHECKDIGITERR");

        entity.setStudentPen(null);
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setStudentPen("2345");
        val validationDigitError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationDigitError.size()).isNotZero();
        assertThat(validationDigitError.get(0).getValidationIssueCode()).isEqualTo("PENCHECKDIGITERR");
    }

    @Test
    void testDuplicatePenRuleFound() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        entity.setStudentPen("120164447");
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);

        val entity2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity2.setStudentPen("120164447");
        entity2.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity2.setUpdateDate(LocalDateTime.now());
        entity2.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity2.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        sdcSchoolCollectionStudentRepository.save(entity2);

        val savedEntityTwo = sdcSchoolCollectionStudentRepository.findById(entity2.getSdcSchoolCollectionStudentID());
        assertThat(savedEntityTwo).isPresent();

        val dupePenCount = sdcSchoolCollectionStudentRepository.countForDuplicateStudentPENs(entity.getSdcSchoolCollection().getSdcSchoolCollectionID(), entity.getStudentPen());
        assertThat(dupePenCount).isEqualTo(1);

        entity.setStudentPen("120164447");
        val validationErrorDupe = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorDupe.size()).isNotZero();
    }

    @Test
    void testHomeSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("HS");
        entity.setEnrolledProgramCodes("05");
        entity.setBandCode(null);

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val vError = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSLANG") && val.getValidationIssueSeverityCode().equals("FUNDING_WARNING"));
        assertThat(vError).isTrue();

        entity.setEnrolledProgramCodes("33");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSIND") && val.getValidationIssueSeverityCode().equals("FUNDING_WARNING"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("40");
        val validationErrorSped = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorSped.size()).isNotZero();
        val error1 = validationErrorSped.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSSPED"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes(null);
        entity.setSpecialEducationCategoryCode("A");
        entity.setSchoolFundingCode("14");
        val validationErrorFunding = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorFunding.size()).isNotZero();
        val fundingError = validationErrorFunding.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSSPED"));
        assertThat(fundingError).isTrue();
    }

    @Test
    void testFundingCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setSchoolFundingCode(null);
        entity.setBandCode(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorNull.size()).isZero();

        entity.setSchoolFundingCode("05");
        val validationError5 = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError5.size()).isNotZero();
        assertThat(validationError5.get(0).getValidationIssueCode()).isEqualTo("FUNDINGCODEINVALID");
    }

    @Test
    void testIndigenousCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        entity.setSchoolFundingCode("20");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setSchoolFundingCode("14");
        val validationErrorCode = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCode.size()).isNotZero();
        val error1 = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("BANDCODEBLANK"));
        assertThat(error1).isTrue();

        entity.setSchoolFundingCode("20");
        entity.setBandCode(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorNull.size()).isNotZero();
        val error2 = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("BANDCODEBLANK"));
        assertThat(error2).isTrue();

        entity.setSchoolFundingCode(null);
        entity.setBandCode("0500");
        val validationErrorFNull = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorFNull.size()).isNotZero();

        entity.setSchoolFundingCode("20");
        entity.setBandCode("0500");
        entity.setNativeAncestryInd("K");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        assertThat(validationErrorInd.get(0).getValidationIssueCode()).isEqualTo("NATIVEINDINVALID");

        entity.setSchoolFundingCode("20");
        entity.setBandCode("0000");
        entity.setNativeAncestryInd("Y");
        val validationErrorInvalid = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInvalid.size()).isNotZero();
        assertThat(validationErrorInvalid.get(0).getValidationIssueCode()).isEqualTo("BANDCODEINVALID");
    }

    @Test
    void testGradeCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("X");
        val validationErrorCode = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCode.size()).isNotZero();
        val error = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("INVALIDGRADECODE"));
        assertThat(error).isTrue();

        entity.setEnrolledGradeCode(null);
        val validationErrorCodeNull = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCodeNull.size()).isNotZero();
        assertThat(validationErrorCodeNull.get(0).getValidationIssueCode()).isEqualTo("INVALIDGRADECODE");

    }

    @Test
    void testNonHSGradeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setEnrolledProgramCodes("15");
        val validationCodeError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeError.size()).isNotZero();
        val error = validationCodeError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEINVALID"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("303141");
        val validationCodeInvalidError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeInvalidError.size()).isNotZero();
        val errorInvalid = validationCodeInvalidError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEINVALID"));
        assertThat(errorInvalid).isTrue();

        entity.setEnrolledProgramCodes("0805");
        val validationCodeErrorCount = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeErrorCount.size()).isNotZero();
        val errorCount = validationCodeErrorCount.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECOUNTERR"));
        assertThat(errorCount).isTrue();

        entity.setEnrolledProgramCodes("14");
        entity.setEnrolledGradeCode("05");
        val validationCodeProgErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeProgErr.size()).isNotZero();
        val errorProg = validationCodeProgErr.stream().noneMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEFRANCOPHONEERR"));
        assertThat(errorProg).isTrue();

        entity.setEnrolledProgramCodes("33");
        entity.setEnrolledGradeCode("06");
        entity.setNativeAncestryInd("N");
        val validationCodeIndErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeIndErr.size()).isNotZero();
        val errorInd = validationCodeIndErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEIND"));
        assertThat(errorInd).isTrue();

        entity.setEnrolledProgramCodes("4041");
        val validationCodeCrrCount = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeCrrCount.size()).isNotZero();
        val errorCarrCount = validationCodeCrrCount.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODECOUNTERR"));
        assertThat(errorCarrCount).isTrue();

        entity.setEnrolledProgramCodes("40");
        entity.setEnrolledGradeCode("01");
        val validationCrrGradeErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCrrGradeErr.size()).isNotZero();
        val errorCarrGradeErr = validationCrrGradeErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEGRADEERR"));
        assertThat(errorCarrGradeErr).isTrue();

        entity.setEnrolledGradeCode("08");
        entity.setSpecialEducationCategoryCode("0");
        val validationCodeSpedErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeSpedErr.size()).isNotZero();
        val errorSpedErr = validationCodeSpedErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("SPEDERR"));
        assertThat(errorSpedErr).isTrue();
    }

    @Test
    void testGAGradeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setEnrolledProgramCodes("40");
        entity.setEnrolledGradeCode("GA");
        val validationCrrGradeErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCrrGradeErr.size()).isNotZero();
        val errorCarrGradeErr = validationCrrGradeErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEGRADEERR"));
        assertThat(errorCarrGradeErr).isFalse();
    }

    @Test
    void testCareerProgramCodeRule() {

        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        entity.setEnrolledProgramCodes(null);
        entity.setCareerProgramCode("XA");
        entity.setEnrolledGradeCode("09");
        val validateNoCareerProgramCodeAndCareerCode = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validateNoCareerProgramCodeAndCareerCode.size()).isNotZero();
        val errorNoCareerProgramCode = validateNoCareerProgramCodeAndCareerCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEPROGERR"));
        assertThat(errorNoCareerProgramCode).isTrue();

        entity.setEnrolledProgramCodes("05");
        entity.setCareerProgramCode("XA");
        entity.setEnrolledGradeCode("08");
        val validationCareerCodeNullErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCareerCodeNullErr.size()).isNotZero();
        val errorCareerNullErr = validationCareerCodeNullErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEPROGERR"));
        assertThat(errorCareerNullErr).isTrue();

        entity.setEnrolledProgramCodes("4040");
        entity.setCareerProgramCode(null);
        entity.setEnrolledGradeCode("08");
        val validationCodeCrrErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCodeCrrErr.size()).isNotZero();
        val errorCarrErr = validationCodeCrrErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEPROGERR"));
        assertThat(errorCarrErr).isTrue();

        entity.setEnrolledProgramCodes("08");
        entity.setCareerProgramCode(null);
        entity.setEnrolledGradeCode("09");
        val validationNoCareerCodeErr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        val errorNoCareer = validationNoCareerCodeErr.stream().noneMatch(val -> val.getValidationIssueCode().equals("CAREERCODEPROGERR"));
        assertThat(errorNoCareer).isTrue();

    }

    @Test
    void testAdultStudentRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("DISTONLINE");

        entity.setDob("0210424F");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("19000101");
        val validationErrorDeadPersonDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorDeadPersonDate.size()).isNotZero();
        assertThat(validationErrorDeadPersonDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob(LocalDate.now().plusYears(2).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        val validationErrorFuturePersonDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorFuturePersonDate.size()).isNotZero();
        assertThat(validationErrorFuturePersonDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        school.setFacilityTypeCode(FacilityTypeCodes.POST_SEC.getCode());
        entity.setDob(LocalDate.now().minusYears(20).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        entity.setNumberOfCourses("0000");
        entity.setEnrolledGradeCode("10");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorOldDate.size()).isNotZero();
        val zeroCoursesInd = validationErrorOldDate.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTZEROCOURSES"));
        assertThat(zeroCoursesInd).isTrue();
        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("ADULTZEROCOURSES");
        school.setFacilityTypeCode("DISTONLINE");

        entity.setDob(LocalDate.now().minusYears(20).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        entity.setNumberOfCourses("0100");
        entity.setEnrolledGradeCode("01");
        val validationErrorGrade = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorGrade.size()).isNotZero();
        val errorInd = validationErrorGrade.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTINCORRECTGRADE"));
        assertThat(errorInd).isTrue();

        entity.setDob(LocalDate.now().minusYears(20).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        entity.setNumberOfCourses("0100");
        entity.setEnrolledGradeCode("01");
        val validationErrorOnlineGrade = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorOnlineGrade.size()).isNotZero();
        val errorOnlineInd = validationErrorOnlineGrade.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTGRADEERR"));
        assertThat(errorOnlineInd).isTrue();
    }

    @Test
    void testHomeLanguageRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("08");
        entity.setHomeLanguageSpokenCode(null);
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInvalidDate.size()).isZero();

        entity.setHomeLanguageSpokenCode("00U");
        val validationErrorLang = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorLang.size()).isNotZero();
        assertThat(validationErrorLang.get(0).getValidationIssueCode()).isEqualTo("SPOKENLANGERR");
    }

    @Test
    void testSchoolAgedStudentRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("STANDARD");

        entity.setDob("0210424F");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("19000101");
        val validationErrorDeadPersonDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorDeadPersonDate.size()).isNotZero();
        assertThat(validationErrorDeadPersonDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob(LocalDate.now().plusYears(2).format(DateTimeFormatter.ofPattern("yyyyMMdd")));
        val validationErrorFuturePersonDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorFuturePersonDate.size()).isNotZero();
        assertThat(validationErrorFuturePersonDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20180101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorOldDate.size()).isZero();

        entity.setDob(LocalDate.now().minusYears(4).format(format));
        school.setFacilityTypeCode("CONT_ED");
        val validationErrorContEd = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorContEd.size()).isNotZero();
        val errorContEd = validationErrorContEd.stream().anyMatch(val -> val.getValidationIssueCode().equals("CONTEDERR"));
        assertThat(errorContEd).isTrue();

        entity.setDob(LocalDate.now().minusYears(1).format(format));
        val validationErrorTooYoung = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorTooYoung.size()).isNotZero();
        val errorTooYoung = validationErrorTooYoung.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.AGE_LESS_THAN_FIVE.getCode()));
        assertThat(errorTooYoung).isTrue();

        entity.setDob(LocalDate.now().minusYears(8).format(format));
        school.setFacilityTypeCode("DIST_LEARN");
        entity.setNumberOfCourses("0400");
        val validationErrorDist = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorDist.size()).isZero();
    }

    @Test
    void testNoOfCoursesRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setNumberOfCourses("0500");
        entity.setEnrolledGradeCode("08");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setNumberOfCourses("1600");
        entity.setEnrolledGradeCode("08");
        val validationErrorMax = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorMax.size()).isNotZero();
        val errorContEd = validationErrorMax.stream().anyMatch(val -> val.getValidationIssueCode().equals("NOOFCOURSEMAX"));
        assertThat(errorContEd).isTrue();
    }

    @Test
    void testHSSchoolAgeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("HS");

        entity.setDob(LocalDateTime.now().minusYears(4).format(format));
        val validationErrorAdult = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorAdult.size()).isNotZero();
        val errorContEd = validationErrorAdult.stream().anyMatch(val -> val.getValidationIssueCode().equals("HSNOTSCHOOLAGE"));
        assertThat(errorContEd).isTrue();

        entity.setDob(LocalDateTime.now().minusYears(6).format(format));
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("HSNOTSCHOOLAGE"));
        assertThat(error).isFalse();
    }

    @Test
    void testFundingCode14Rule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("01");
        entity.setSchoolFundingCode("14");
        entity.setBandCode(null);
        entity.setEnrolledProgramCodes("05");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEFUNDINGERR"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("33");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEINDERR"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes("XA");
        val validationErrorCarrCodes = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCarrCodes.size()).isNotZero();
        val error2 = validationErrorCarrCodes.stream().noneMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECAREERERR"));
        assertThat(error2).isTrue();

        entity.setEnrolledProgramCodes("40");
        val validationErrorCarr = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorCarr.size()).isNotZero();
        val error3 = validationErrorCarr.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECAREERERR"));
        assertThat(error3).isTrue();
    }

    @Test
    void testOutOfProvinceSpecialEducRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("01");
        entity.setSchoolFundingCode("14");
        entity.setBandCode(null);
        entity.setSpecialEducationCategoryCode("A");

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isEqualTo(2);
        val error1 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ENROLLED_CODE_SP_ED_ERR.getCode())
                && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.SCHOOL_FUNDING_CODE.getCode()));
        val error2 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ENROLLED_CODE_SP_ED_ERR.getCode())
                && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.SPECIAL_EDUCATION_CATEGORY_CODE.getCode()));
        assertThat(error1).isTrue();
        assertThat(error2).isTrue();
    }

    @Test
    void testSchoolAgedIndigenousSupportRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val entity2 = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledProgramCodes("33");
        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        entity2.setEnrolledProgramCodes("33");
        entity2.setDob(LocalDateTime.now().minusYears(4).format(format));

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        val error1 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.ENROLLED_PROGRAM_CODE.getCode()));
        val error2 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.DOB.getCode()));
        assertThat(error1).isTrue();
        assertThat(error2).isTrue();

        val validationError2 = rulesProcessor.processRules(createMockStudentRuleData(entity2, createMockSchool()));
        val error21 = validationError2.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.ENROLLED_PROGRAM_CODE.getCode()));
        val error22 = validationError2.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_INDIGENOUS_SUPPORT.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.DOB.getCode()));
        assertThat(error21).isTrue();
        assertThat(error22).isTrue();
    }

    @Test
    void testSchoolAgedELLRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val entity2 = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledProgramCodes("17");
        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        entity2.setEnrolledProgramCodes("17");
        entity2.setDob(LocalDateTime.now().minusYears(4).format(format));

        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        val error1 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_ELL.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.ENROLLED_PROGRAM_CODE.getCode()));
        val error2 = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_ELL.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.DOB.getCode()));
        assertThat(error1).isTrue();
        assertThat(error2).isTrue();

        val validationError2 = rulesProcessor.processRules(createMockStudentRuleData(entity2, createMockSchool()));
        val error21 = validationError2.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_ELL.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.ENROLLED_PROGRAM_CODE.getCode()));
        val error22 = validationError2.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_ELL.getCode())
            && val.getValidationIssueFieldCode().equals(StudentValidationFieldCode.DOB.getCode()));
        assertThat(error21).isTrue();
        assertThat(error22).isTrue();
    }

    @Test
    void testSchoolAgedSpedRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));

        var graduatedAdult = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        graduatedAdult.setSpecialEducationCategoryCode("A");
        graduatedAdult.setDob(LocalDateTime.now().minusYears(20).format(format));
        graduatedAdult.setIsGraduated(true);
        graduatedAdult.setEnrolledGradeCode("10");

        var nonGraduatedAdultInGA = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        nonGraduatedAdultInGA.setSpecialEducationCategoryCode("A");
        nonGraduatedAdultInGA.setDob(LocalDateTime.now().minusYears(20).format(format));
        nonGraduatedAdultInGA.setIsGraduated(false);
        nonGraduatedAdultInGA.setEnrolledGradeCode("GA");

        var notSchoolAged = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        notSchoolAged.setSpecialEducationCategoryCode("A");
        notSchoolAged.setDob(LocalDateTime.now().minusYears(2).format(format));

        var validationErrorGraduatedAdult = rulesProcessor.processRules(createMockStudentRuleData(graduatedAdult, createMockSchool()));
        boolean errorGraduatedAdult = validationErrorGraduatedAdult.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_SPED.getCode()));

        var validationErrorNonGraduatedAdultInGA = rulesProcessor.processRules(createMockStudentRuleData(nonGraduatedAdultInGA, createMockSchool()));
        boolean errorNonGraduatedAdultInGA = validationErrorNonGraduatedAdultInGA.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_SPED.getCode()));

        var validationErrorNotSchoolAged = rulesProcessor.processRules(createMockStudentRuleData(notSchoolAged, createMockSchool()));
        boolean errorNotSchoolAged = validationErrorNotSchoolAged.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_SPED.getCode()));

        assertThat(errorGraduatedAdult).isTrue();
        assertThat(errorNonGraduatedAdultInGA).isTrue();
        assertThat(errorNotSchoolAged).isTrue();
    }

    @Test
    void testAdultGraduatesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_GRADUATED.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testIndependentSchoolGraduateStudentRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());

        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsAdult(true);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.GRADUATE_STUDENT_INDEPENDENT.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSchoolAgedGraduatesSummerRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        val saga = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUMMER.getCode()));
        assertThat(error).isTrue();
    }

    @Test
    void testSchoolAgedGraduatesSupportRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledGradeCode("10");
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(true);
        saga.getSdcSchoolCollectionStudentEntity().setSupportBlocks("1");

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS.getCode()));
        assertThat(error).isTrue();

        saga.getSdcSchoolCollectionStudentEntity().setSupportBlocks("0");
        val validationRuleWithZeroSupportBlock = rulesProcessor.processRules(saga);
        assertThat(validationRuleWithZeroSupportBlock.size()).isZero();

        saga.getSdcSchoolCollectionStudentEntity().setEnrolledGradeCode("07");
        val validationGradRule2 = rulesProcessor.processRules(saga);
        val error2 = validationGradRule2.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.SCHOOL_AGED_GRADUATE_SUPPORT_BLOCKS.getCode()));
        assertThat(error2).isFalse();
    }

    @Test
    void testSummerSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        val saga = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());

        val validationErrorDOB = rulesProcessor.processRules(saga);
        assertThat(validationErrorDOB.size()).isNotZero();
        val error = validationErrorDOB.stream().anyMatch(val -> val.getValidationIssueCode().equals("STUDENTADULTERR"));
        assertThat(error).isTrue();

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setSupportBlocks("1");
        val saga2 = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());

        val validationErrorSupportBlock = rulesProcessor.processRules(saga2);
        assertThat(validationErrorSupportBlock.size()).isNotZero();
        val error1 = validationErrorSupportBlock.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUMMERSUPPORTBLOCKSNA"));
        assertThat(error1).isTrue();

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setSupportBlocks("0");
        entity.setEnrolledGradeCode("HS");
        val saga3 = createMockStudentRuleData(entity, school);
        collection.setCollectionTypeCode(JULY.getTypeCode());

        val validationErrorGradeErr = rulesProcessor.processRules(saga3);
        assertThat(validationErrorGradeErr.size()).isNotZero();
        val errorGrade = validationErrorGradeErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUMMERGRADECODE"));
        assertThat(errorGrade).isTrue();
    }

    @Test
    void testOffshoreSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode("OFFSHORE");
    }

    @Test
    void testOtherCoursesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setOtherCourses(null);
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("OTHERCOURSEINVALID"));
        assertThat(error).isFalse();

        entity.setOtherCourses("10");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("OTHERCOURSEINVALID"));
        assertThat(error1).isTrue();
    }

    @Test
    void testPostalCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setPostalCode(null);
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("MISSINGPOSTALCODE"));
        assertThat(error).isTrue();

        val school = createMockSchool();
        school.setSchoolCategoryCode("OFFSHORE");
        entity.setPostalCode(null);
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorInd.size()).isZero();

        entity.setPostalCode("11111");
        val validationPattern = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationPattern.size()).isNotZero();
        val errorPatt = validationPattern.stream().anyMatch(val -> val.getValidationIssueCode().equals("INVALIDPOSTALCODE"));
        assertThat(errorPatt).isTrue();
    }

    @Test
    void testSupportBlockRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setSupportBlocks("9");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUPPORTBLOCKSINVALID"));
        assertThat(error).isTrue();

        val school = createMockSchool();
        school.setFacilityTypeCode("DISTONLINE");
        entity.setSupportBlocks("9");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().noneMatch(val -> val.getValidationIssueCode().equals("SUPPORTFACILITYNA"));
        assertThat(error1).isTrue();

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setEnrolledGradeCode("GA");
        val validationDOB = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationDOB.size()).isNotZero();
        val errorDOB = validationDOB.stream().noneMatch(val -> val.getValidationIssueCode().equals("GAERROR"));
        assertThat(errorDOB).isTrue();

        entity.setNumberOfCourses(null);
        val validationCourses = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationCourses.size()).isNotZero();
        val errorCourses = validationCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUPPORTBLOCKSNOTCOUNT"));
        assertThat(errorCourses).isFalse();

        entity.setNumberOfCourses("0800");
        entity.setSupportBlocks("0");
        entity.setEnrolledGradeCode("10");
        val validationSupportBlocks = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationSupportBlocks.size()).isZero();
        val errorSupport = validationCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUPPORTBLOCKSNOTCOUNT"));
        assertThat(errorSupport).isFalse();
    }

    @Test
    void testKHRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("KH");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().noneMatch(val -> val.getValidationIssueCode().equals("KHGRADECODEINVALID"));
        assertThat(error).isTrue();
    }

    @Test
    void testAdultOnlineZeroCourseHistory(){
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = createMockSchool();
        school.setFacilityTypeCode(String.valueOf(DISTONLINE));
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        UUID oneYearOldCollectionID = createMockHistoricalCollection(1, entity.getSdcSchoolCollection().getSchoolID(), entity.getCreateDate(), String.valueOf(SEPTEMBER));
        UUID twoYearOldCollectionID = createMockHistoricalCollection(2, entity.getSdcSchoolCollection().getSchoolID(), entity.getCreateDate(), String.valueOf(JULY));

        entity.setIsAdult(true);
        entity.setIsSchoolAged(false);
        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        entity.setEnrolledGradeCode("10");
        entity.setSpecialEducationCategoryCode(null);
        val validationNoErrorAdultWithClasses = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationNoErrorAdultWithClasses.size()).isZero();

        entity.setNumberOfCourses("0000");
        val validationNoErrorAdultWithHistory = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationNoErrorAdultWithHistory.size()).isZero();

        sdcSchoolCollectionRepository.deleteById(oneYearOldCollectionID);
        sdcSchoolCollectionRepository.deleteById(twoYearOldCollectionID);

        val validationErrorAdult = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        val errorAdult = validationErrorAdult.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTZEROCOURSEH"));
        assertThat(errorAdult).isTrue();
    }

    @Test
    void testSchoolAgedOnlineZeroCourseHistory(){
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = createMockSchool();
        school.setFacilityTypeCode(String.valueOf(DIST_LEARN));
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        UUID oneYearOldCollectionID = createMockHistoricalCollection(1, entity.getSdcSchoolCollection().getSchoolID(), entity.getCreateDate(), String.valueOf(SEPTEMBER));
        UUID twoYearOldCollectionID = createMockHistoricalCollection(2, entity.getSdcSchoolCollection().getSchoolID(), entity.getCreateDate(), String.valueOf(JULY));

        entity.setIsAdult(false);
        entity.setIsSchoolAged(true);
        entity.setEnrolledGradeCode("08");
        val validationNoErrorSchlAged = rulesProcessor.processRules((createMockStudentRuleData(entity, school)));
        assertThat(validationNoErrorSchlAged.size()).isZero();

        entity.setEnrolledGradeCode("08");
        entity.setNumberOfCourses("0000");
        val validationNoErrorSchlAgedWithHistory = rulesProcessor.processRules((createMockStudentRuleData(entity, school)));
        val errorSchlAgedWithHistory = validationNoErrorSchlAgedWithHistory.stream().anyMatch(val -> val.getValidationIssueCode().equals("SCHOOLAGEDZEROCOURSEH"));
        assertThat(errorSchlAgedWithHistory).isFalse();

        sdcSchoolCollectionRepository.deleteById(oneYearOldCollectionID);
        sdcSchoolCollectionRepository.deleteById(twoYearOldCollectionID);

        val validationErrorSchlAged = rulesProcessor.processRules((createMockStudentRuleData(entity, school)));
        val errorSchlAged = validationErrorSchlAged.stream().anyMatch(val -> val.getValidationIssueCode().equals("SCHOOLAGEDZEROCOURSEH"));
        assertThat(errorSchlAged).isTrue();

        entity.setEnrolledGradeCode("01");
        val validationNoErrorSchlAgedYounger = rulesProcessor.processRules((createMockStudentRuleData(entity, school)));
        assertThat(validationNoErrorSchlAgedYounger.size()).isZero();
    }

    @Test
    void testAdultStudentGradeRule_WithGrade08() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("DISTONLINE");

        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        entity.setNumberOfCourses("0100");
        entity.setEnrolledGradeCode("08");
        val validationErrorGrade = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationErrorGrade.size()).isNotZero();
        val errorInd = validationErrorGrade.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTINCORRECTGRADE"));
        assertThat(errorInd).isFalse();

    }

    @Test
    void testSchoolAgedWithZeroNoOfCoursesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("STANDARD");

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setNumberOfCourses("0000");
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("SCHOOLAGEZEROCOURSES"));
        assertThat(error).isTrue();
    }

    @Test
    void testSchoolAgedWithNullNoOfCoursesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("STANDARD");

        entity.setDob(LocalDateTime.now().minusYears(8).format(format));
        entity.setNumberOfCourses(null);
        val validationError = rulesProcessor.processRules(createMockStudentRuleData(entity, school));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("SCHOOLAGEZEROCOURSES"));
        assertThat(error).isTrue();
    }

    @Test
    void testAdultStudentSupportBlockRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());

        entity.setDob(LocalDateTime.now().minusYears(20).format(format));
        entity.setEnrolledGradeCode("10");
        entity.setSpecialEducationCategoryCode(null);
        val saga = createMockStudentRuleData(entity, school);
        saga.getSdcSchoolCollectionStudentEntity().setIsGraduated(false);
        saga.getSdcSchoolCollectionStudentEntity().setSupportBlocks("1");

        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        val validationGradRule = rulesProcessor.processRules(saga);
        assertThat(validationGradRule.size()).isNotZero();
        val error = validationGradRule.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_SUPPORT_ERR.getCode()));
        assertThat(error).isTrue();

        saga.getSdcSchoolCollectionStudentEntity().setSupportBlocks("0");
        val validationRuleWithZeroSupportBlock = rulesProcessor.processRules(saga);
        assertThat(validationRuleWithZeroSupportBlock.size()).isZero();
    }

    @Test
    void testAdultStudentCoursesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        var school = createMockSchool();
        school.setFacilityTypeCode(FacilityTypeCodes.POST_SEC.getCode());

        var adultWithoutCoursesSet = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        adultWithoutCoursesSet.setDob(LocalDateTime.now().minusYears(20).format(format));
        adultWithoutCoursesSet.setEnrolledGradeCode("10");
        adultWithoutCoursesSet.setNumberOfCourses("");
        var testNullCourses = rulesProcessor.processRules((createMockStudentRuleData(adultWithoutCoursesSet, school)));
        boolean nullCoursesValidation = testNullCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_ZERO_COURSES.getCode()));

        var adultWithoutCourses = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        adultWithoutCourses.setDob(LocalDateTime.now().minusYears(20).format(format));
        adultWithoutCourses.setEnrolledGradeCode("10");
        adultWithoutCourses.setNumberOfCourses("0");
        var testZeroCourses = rulesProcessor.processRules((createMockStudentRuleData(adultWithoutCourses, school)));
        boolean zeroCoursesValidation = testZeroCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_ZERO_COURSES.getCode()));

        var adultWithCourses = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        adultWithCourses.setDob(LocalDateTime.now().minusYears(20).format(format));
        adultWithCourses.setEnrolledGradeCode("10");
        adultWithCourses.setNumberOfCourses("3");
        var validationErrorAdultWithCourses = rulesProcessor.processRules(createMockStudentRuleData(adultWithCourses, school));
        boolean errorAdultWithCourses = validationErrorAdultWithCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_ZERO_COURSES.getCode()));

        school.setFacilityTypeCode(DISTONLINE.getCode());
        var testDistOnlineWithNoCourses = rulesProcessor.processRules((createMockStudentRuleData(adultWithoutCourses, school)));
        boolean testDistOnlineWithNoCoursesValidation = testDistOnlineWithNoCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.ADULT_ZERO_COURSES.getCode()));

        assertThat(nullCoursesValidation).isTrue();
        assertThat(zeroCoursesValidation).isTrue();
        assertThat(errorAdultWithCourses).isFalse();
        assertThat(testDistOnlineWithNoCoursesValidation).isFalse();
    }

    @Test
    void testRefugeeFundingRule_inSept() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSept = createMockCollectionEntity();
        collectionSept.setCloseDate(now.minusDays(5));
        collectionSept.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSept.setSnapshotDate(currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(30));
        collectionSept.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSept);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSept = createMockSdcDistrictCollectionEntity(collectionSept, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSept);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSept = createMockSdcSchoolCollectionEntity(collectionSept, schoolId);
        sdcMockSchoolSept.setUploadDate(null);
        sdcMockSchoolSept.setUploadFileName(null);
        sdcMockSchoolSept.setSdcDistrictCollectionID(sdcDistrictCollectionSept.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSept);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSept = createMockSchoolStudentEntity(sdcMockSchoolSept);
        studSept.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSept);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }


    @Test
    void testRefugeeFundingRule_notInSept() {
        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionRepository.save(collectionFeb);

        SchoolTombstone schoolFeb = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId()));
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        var sdcSchoolCollectionEntityFeb = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId())));

        var studFeb = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        sdcSchoolCollectionStudentRepository.save(studFeb);

        var validateNoErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, schoolFeb));
        boolean errorRefugeeFunding = validateNoErrorRefugeeFunding.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isFalse();
    }

    @Test
    void testRefugeeFundingRule_nonEligibleFacilityType() {
        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionRepository.save(collectionFeb);

        SchoolTombstone schoolFeb = createMockSchool();
        schoolFeb.setFacilityTypeCode(FacilityTypeCodes.JUSTB4PRO.getCode());
        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId()));
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        var sdcSchoolCollectionEntityFeb = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId())));

        var studFeb = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        sdcSchoolCollectionStudentRepository.save(studFeb);

        var validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, schoolFeb));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_nonPublicSchool() {
        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionRepository.save(collectionFeb);

        SchoolTombstone schoolFeb = createMockSchool();
        schoolFeb.setSchoolCategoryCode(SchoolCategoryCodes.INDEPEND.getCode());
        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId()));
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        var studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        sdcSchoolCollectionStudentRepository.save(studFeb);

        var validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, schoolFeb));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inPreviousSeptTwoYearsAgo() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSeptTwoYearsAgo = createMockCollectionEntity();
        collectionSeptTwoYearsAgo.setCloseDate(now.minusYears(2).minusDays(5));
        collectionSeptTwoYearsAgo.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSeptTwoYearsAgo.setSnapshotDate(currentSnapshotDate.minusYears(2).withMonth(9).withDayOfMonth(30));
        collectionSeptTwoYearsAgo.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSeptTwoYearsAgo);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSeptTwoYearsAgo = createMockSdcDistrictCollectionEntity(collectionSeptTwoYearsAgo, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSeptTwoYearsAgo);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSeptTwoYearsAgo = createMockSdcSchoolCollectionEntity(collectionSeptTwoYearsAgo, schoolId);
        sdcMockSchoolSeptTwoYearsAgo.setUploadDate(null);
        sdcMockSchoolSeptTwoYearsAgo.setUploadFileName(null);
        sdcMockSchoolSeptTwoYearsAgo.setSdcDistrictCollectionID(sdcDistrictCollectionSeptTwoYearsAgo.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSeptTwoYearsAgo);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSeptTwoYearsAgo = createMockSchoolStudentEntity(sdcMockSchoolSeptTwoYearsAgo);
        studSeptTwoYearsAgo.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSeptTwoYearsAgo);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inPreviousSeptTwoYearsAgo_septCollectionNotCompleted() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSeptTwoYearsAgo = createMockCollectionEntity();
        collectionSeptTwoYearsAgo.setCloseDate(now.minusYears(2).minusDays(5));
        collectionSeptTwoYearsAgo.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSeptTwoYearsAgo.setSnapshotDate(currentSnapshotDate.minusYears(2).withMonth(9).withDayOfMonth(30));
        collectionRepository.save(collectionSeptTwoYearsAgo);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSeptTwoYearsAgo = createMockSdcDistrictCollectionEntity(collectionSeptTwoYearsAgo, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSeptTwoYearsAgo);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSeptTwoYearsAgo = createMockSdcSchoolCollectionEntity(collectionSeptTwoYearsAgo, schoolId);
        sdcMockSchoolSeptTwoYearsAgo.setUploadDate(null);
        sdcMockSchoolSeptTwoYearsAgo.setUploadFileName(null);
        sdcMockSchoolSeptTwoYearsAgo.setSdcDistrictCollectionID(sdcDistrictCollectionSeptTwoYearsAgo.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSeptTwoYearsAgo);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSeptTwoYearsAgo = createMockSchoolStudentEntity(sdcMockSchoolSeptTwoYearsAgo);
        studSeptTwoYearsAgo.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSeptTwoYearsAgo);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .noneMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inJuly() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSept = createMockCollectionEntity();
        collectionSept.setCloseDate(now.minusDays(5));
        collectionSept.setCollectionTypeCode(CollectionTypeCodes.JULY.getTypeCode());
        collectionSept.setSnapshotDate(currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(30));
        collectionSept.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSept);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSept = createMockSdcDistrictCollectionEntity(collectionSept, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSept);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSept = createMockSdcSchoolCollectionEntity(collectionSept, schoolId);
        sdcMockSchoolSept.setUploadDate(null);
        sdcMockSchoolSept.setUploadFileName(null);
        sdcMockSchoolSept.setSdcDistrictCollectionID(sdcDistrictCollectionSept.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSept);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSept = createMockSchoolStudentEntity(sdcMockSchoolSept);
        studSept.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSept);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inMay() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSept = createMockCollectionEntity();
        collectionSept.setCloseDate(now.minusDays(5));
        collectionSept.setCollectionTypeCode(CollectionTypeCodes.MAY.getTypeCode());
        collectionSept.setSnapshotDate(currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(30));
        collectionSept.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSept);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSept = createMockSdcDistrictCollectionEntity(collectionSept, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSept);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSept = createMockSdcSchoolCollectionEntity(collectionSept, schoolId);
        sdcMockSchoolSept.setUploadDate(null);
        sdcMockSchoolSept.setUploadFileName(null);
        sdcMockSchoolSept.setSdcDistrictCollectionID(sdcDistrictCollectionSept.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSept);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSept = createMockSchoolStudentEntity(sdcMockSchoolSept);
        studSept.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSept);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_isAdult() {
        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionRepository.save(collectionFeb);

        SchoolTombstone schoolFeb = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId()));
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        var sdcSchoolCollectionEntityFeb = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collectionFeb, UUID.fromString(schoolFeb.getSchoolId())));

        var studFeb = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntityFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setDob(LocalDateTime.now().minusYears(20).format(format));
        sdcSchoolCollectionStudentRepository.save(studFeb);

        var validateNoErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, schoolFeb));
        boolean errorRefugeeFunding = validateNoErrorRefugeeFunding.stream().anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IS_ADULT.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inSept_isAdult_doesNotExecute() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSept = createMockCollectionEntity();
        collectionSept.setCloseDate(now.minusDays(5));
        collectionSept.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSept.setSnapshotDate(currentSnapshotDate.minusYears(1).withMonth(9).withDayOfMonth(30));
        collectionSept.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSept);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSept = createMockSdcDistrictCollectionEntity(collectionSept, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSept);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSept = createMockSdcSchoolCollectionEntity(collectionSept, schoolId);
        sdcMockSchoolSept.setUploadDate(null);
        sdcMockSchoolSept.setUploadFileName(null);
        sdcMockSchoolSept.setSdcDistrictCollectionID(sdcDistrictCollectionSept.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSept);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSept = createMockSchoolStudentEntity(sdcMockSchoolSept);
        studSept.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSept);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        studFeb.setDob(LocalDateTime.now().minusYears(20).format(format));
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .noneMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inPreviousSeptTwoYearsAgo_inDiffDistrictAndDiffSchool() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSeptTwoYearsAgo = createMockCollectionEntity();
        collectionSeptTwoYearsAgo.setCloseDate(now.minusYears(2).minusDays(5));
        collectionSeptTwoYearsAgo.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSeptTwoYearsAgo.setSnapshotDate(currentSnapshotDate.minusYears(2).withMonth(9).withDayOfMonth(30));
        collectionSeptTwoYearsAgo.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSeptTwoYearsAgo);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        SchoolTombstone  school2 = createMockSchool();
        District district = createMockDistrict();
        District district2 = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school2.setDistrictId(district2.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        UUID schoolId2 = UUID.fromString(school2.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSeptTwoYearsAgo = createMockSdcDistrictCollectionEntity(collectionSeptTwoYearsAgo, UUID.fromString(district2.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSeptTwoYearsAgo);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSeptTwoYearsAgo = createMockSdcSchoolCollectionEntity(collectionSeptTwoYearsAgo, schoolId2);
        sdcMockSchoolSeptTwoYearsAgo.setUploadDate(null);
        sdcMockSchoolSeptTwoYearsAgo.setUploadFileName(null);
        sdcMockSchoolSeptTwoYearsAgo.setSdcDistrictCollectionID(sdcDistrictCollectionSeptTwoYearsAgo.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSeptTwoYearsAgo);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSeptTwoYearsAgo = createMockSchoolStudentEntity(sdcMockSchoolSeptTwoYearsAgo);
        studSeptTwoYearsAgo.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSeptTwoYearsAgo);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }

    @Test
    void testRefugeeFundingRule_inPreviousSeptTwoYearsAgo_inDiffSchoolSameDis() {
        LocalDateTime now = LocalDateTime.now();
        LocalDate currentSnapshotDate = now.toLocalDate();

        CollectionEntity collectionSeptTwoYearsAgo = createMockCollectionEntity();
        collectionSeptTwoYearsAgo.setCloseDate(now.minusYears(2).minusDays(5));
        collectionSeptTwoYearsAgo.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collectionSeptTwoYearsAgo.setSnapshotDate(currentSnapshotDate.minusYears(2).withMonth(9).withDayOfMonth(30));
        collectionSeptTwoYearsAgo.setCollectionStatusCode("COMPLETED");
        collectionRepository.save(collectionSeptTwoYearsAgo);

        CollectionEntity collectionFeb = createMockCollectionEntity();
        collectionFeb.setCloseDate(now.plusDays(2));
        collectionFeb.setCollectionTypeCode(CollectionTypeCodes.FEBRUARY.getTypeCode());
        collectionFeb.setSnapshotDate(currentSnapshotDate);
        collectionRepository.save(collectionFeb);

        SchoolTombstone school = createMockSchool();
        SchoolTombstone school2 = createMockSchool();
        District district = createMockDistrict();
        school.setDistrictId(district.getDistrictId());
        school2.setDistrictId(district.getDistrictId());
        UUID schoolId = UUID.fromString(school.getSchoolId());
        UUID schoolId2 = UUID.fromString(school2.getSchoolId());

        SdcDistrictCollectionEntity sdcDistrictCollectionSeptTwoYearsAgo = createMockSdcDistrictCollectionEntity(collectionSeptTwoYearsAgo, UUID.fromString(district.getDistrictId()));
        SdcDistrictCollectionEntity sdcDistrictCollectionFeb = createMockSdcDistrictCollectionEntity(collectionFeb, UUID.fromString(district.getDistrictId()));
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionSeptTwoYearsAgo);
        sdcDistrictCollectionRepository.save(sdcDistrictCollectionFeb);

        SdcSchoolCollectionEntity sdcMockSchoolSeptTwoYearsAgo = createMockSdcSchoolCollectionEntity(collectionSeptTwoYearsAgo, schoolId);
        sdcMockSchoolSeptTwoYearsAgo.setUploadDate(null);
        sdcMockSchoolSeptTwoYearsAgo.setUploadFileName(null);
        sdcMockSchoolSeptTwoYearsAgo.setSdcDistrictCollectionID(sdcDistrictCollectionSeptTwoYearsAgo.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolSeptTwoYearsAgo);

        SdcSchoolCollectionEntity sdcMockSchoolFeb = createMockSdcSchoolCollectionEntity(collectionFeb, schoolId2);
        sdcMockSchoolFeb.setUploadDate(null);
        sdcMockSchoolFeb.setUploadFileName(null);
        sdcMockSchoolFeb.setSdcDistrictCollectionID(sdcDistrictCollectionFeb.getSdcDistrictCollectionID());
        sdcSchoolCollectionRepository.save(sdcMockSchoolFeb);

        UUID assignedStudentId = UUID.randomUUID();
        SdcSchoolCollectionStudentEntity studSeptTwoYearsAgo = createMockSchoolStudentEntity(sdcMockSchoolSeptTwoYearsAgo);
        studSeptTwoYearsAgo.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studSeptTwoYearsAgo);

        SdcSchoolCollectionStudentEntity studFeb = createMockSchoolStudentEntity(sdcMockSchoolFeb);
        studFeb.setSchoolFundingCode(SchoolFundingCodes.NEWCOMER_REFUGEE.getCode());
        studFeb.setAssignedStudentId(assignedStudentId);
        sdcSchoolCollectionStudentRepository.save(studFeb);

        List<SdcSchoolCollectionStudentValidationIssue> validationErrorRefugeeFunding = rulesProcessor.processRules(createMockStudentRuleData(studFeb, school));
        boolean errorRefugeeFunding = validationErrorRefugeeFunding.stream()
                .anyMatch(val -> val.getValidationIssueCode().equals(StudentValidationIssueTypeCode.REFUGEE_IN_PREV_COL.getCode()));

        assertThat(errorRefugeeFunding).isTrue();
    }
}
