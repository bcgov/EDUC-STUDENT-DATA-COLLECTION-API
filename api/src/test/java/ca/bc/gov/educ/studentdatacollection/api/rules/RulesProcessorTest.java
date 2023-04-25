package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class RulesProcessorTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private RulesProcessor rulesProcessor;

    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    @Autowired
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

    @AfterEach
    public void afterEach() {
        this.collectionRepository.deleteAll();
        this.sdcSchoolCollectionRepository.deleteAll();
        this.sdcSchoolCollectionStudentRepository.deleteAll();
    }

    @Test
    void testGenderRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setGender("M");
        val validationErrorM = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorM.size()).isZero();

        entity.setGender("F");
        val validationErrorF = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorF.size()).isZero();

        entity.setGender("U");
        val validationErrorIncorrectVal = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorIncorrectVal.size()).isNotZero();
        assertThat(validationErrorIncorrectVal.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

        entity.setGender(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorNull.size()).isNotZero();
        assertThat(validationErrorNull.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

        entity.setGender("");
        val validationErrorEmpty = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorEmpty.size()).isNotZero();
        assertThat(validationErrorEmpty.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");

    }

    @Test
    void testDOBRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setDob("20230230");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("0210424F");
        val validationErrorInvalidCharDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInvalidCharDate.size()).isNotZero();
        assertThat(validationErrorInvalidCharDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        entity.setDob(format.format(LocalDate.now().plusDays(2)));
        val validationErrorFutureDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorFutureDate.size()).isNotZero();
        assertThat(validationErrorFutureDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("18990101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorOldDate.size()).isNotZero();
        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob(null);
        val validationErrorNullDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorNullDate.size()).isNotZero();
        assertThat(validationErrorNullDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("");
        val validationErrorEmptyDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorEmptyDate.size()).isNotZero();
        assertThat(validationErrorEmptyDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20180420");
        val validationErrorCorrectDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCorrectDate.size()).isZero();
    }

    @Test
    void testLocalIDRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLocalID(null);
        val validationErrorBlank = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorBlank.size()).isNotZero();
        assertThat(validationErrorBlank.get(0).getValidationIssueFieldCode()).isEqualTo("LOCALID");
    }

    @Test
    void testSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationError.size()).isZero();

        val sagaData = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school);
        sagaData.setCollectionTypeCode("JULY");

        sdcSchoolCollectionStudentRepository.save(entity);
        val validationErrorBlank = rulesProcessor.processRules(sagaData);
        assertThat(validationErrorBlank.size()).isNotZero();
        assertThat(validationErrorBlank.get(0).getValidationIssueFieldCode()).isEqualTo("STUDENT_PEN");
    }

    @Test
    void testStudentLegalLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalLastName(null);
        val nullLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(nullLastNameErr.size()).isNotZero();
        assertThat(nullLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBLANK");

        entity.setLegalLastName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMECHARFIX");

        entity.setLegalLastName("FAKE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBADVALUE");
    }

    @Test
    void testStudentUsualLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualLastName("Bob$");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALLASTNAMECHARFIX");

        entity.setUsualLastName("FAKE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALLASTNAMEBADVALUE");

        entity.setUsualLastName(null);
        val nullLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(nullLastNameErr.size()).isZero();
    }

    @Test
    void testStudentLegalFirstNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalFirstName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALFIRSTNAMECHARFIX");

        entity.setLegalFirstName("DELETE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALFIRSTNAMEBADVALUE");
    }

    @Test
    void testStudentUsualFirstNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualFirstName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALFIRSTNAMECHARFIX");

        entity.setUsualFirstName("NOTAPPLICABLE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALFIRSTNAMEBADVALUE");
    }

    @Test
    void testStudentLegalMiddleNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalMiddleNames("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALMIDDLENAMECHARFIX");

        entity.setLegalMiddleNames("TeSTStuD");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALMIDDLENAMEBADVALUE");
    }

    @Test
    void testStudentUsualMiddleNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setUsualMiddleNames("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALMIDDLENAMECHARFIX");

        entity.setUsualMiddleNames("na");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("USUALMIDDLENAMEBADVALUE");
    }

    @Test
    void testCSFProgramRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("14");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo("ENROLLEDCODEPARSEERR");

        entity.setEnrolledProgramCodes("0000000000000000");
        val validationCodeError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationCodeError.size()).isNotZero();
        assertThat(validationCodeError.get(0).getValidationIssueCode()).isEqualTo("ENROLLEDNOFRANCOPHONE");

        entity.setEnrolledProgramCodes("0000000000000005");
        school.setSchoolReportingRequirementCode("RT");
        val validationCodeReportError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationCodeReportError.size()).isNotZero();
        assertThat(validationCodeReportError.get(0).getValidationIssueCode()).isEqualTo("ENROLLEDWRONGREPORTING");

        entity.setEnrolledProgramCodes("0000000000000005");
        val noValidationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(noValidationError.size()).isZero();
    }

    @Test
    void testPenRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        sdcSchoolCollectionStudentRepository.save(entity);

        val savedEntityOne = sdcSchoolCollectionStudentRepository.findById(entity.getSdcSchoolCollectionStudentID());
        assertThat(savedEntityOne).isPresent();

        val entity2 = createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setCreateDate(LocalDateTime.now().minusMinutes(14));
        entity.setUpdateDate(LocalDateTime.now());
        entity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        sdcSchoolCollectionStudentRepository.save(entity2);

        val savedEntityTwo = sdcSchoolCollectionStudentRepository.findById(entity2.getSdcSchoolCollectionStudentID());
        assertThat(savedEntityTwo).isPresent();

        val dupePenCount = sdcSchoolCollectionStudentRepository.countForDuplicateStudentPENs(entity.getSdcSchoolCollectionID(), entity.getStudentPen());
        assertThat(dupePenCount).isEqualTo(2);

        entity.setStudentPen("123456789");
        val validationErrorDupe = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorDupe.size()).isNotZero();
        assertThat(validationErrorDupe.get(0).getValidationIssueCode()).isEqualTo("STUDENTPENDUPLICATE");

        entity.setStudentPen(null);
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo("STUDENTPENDUPLICATE");
    }
}
