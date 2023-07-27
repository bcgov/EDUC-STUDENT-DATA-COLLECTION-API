package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
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

    @Test
    void testGenderRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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

//        entity.setDob(null);
//        val validationErrorNullDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
//        assertThat(validationErrorNullDate.size()).isNotZero();
//        assertThat(validationErrorNullDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("");
        val validationErrorEmptyDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorEmptyDate.size()).isNotZero();
        assertThat(validationErrorEmptyDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20170420");
        val validationErrorCorrectDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCorrectDate.size()).isZero();
    }

    @Test
    void testLocalIDRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationError.size()).isZero();

        entity.setStudentPen(null);
        val sagaData = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school);
        sagaData.setCollectionTypeCode("JULY");
        val validationErrorBlank = rulesProcessor.processRules(sagaData);
        assertThat(validationErrorBlank.size()).isNotZero();
        assertThat(validationErrorBlank.get(0).getValidationIssueFieldCode()).isEqualTo("STUDENT_PEN");
    }

    @Test
    void testStudentLegalLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setLegalLastName("Billie-Jean");
        val validationErrorNum = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorNum.size()).isZero();

        entity.setLegalLastName(null);
        val nullLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(nullLastNameErr.size()).isNotZero();
        assertThat(nullLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBLANK");

        entity.setLegalLastName("Böb");
        val charsLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameErr.size()).isNotZero();
        assertThat(charsLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMECHARFIX");

        entity.setLegalLastName("'-.");
        val charsLastNameCharErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(charsLastNameCharErr.size()).isNotZero();
        assertThat(charsLastNameCharErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMECHARFIX");

        entity.setLegalLastName("FAKE");
        val badLastNameErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(badLastNameErr.size()).isNotZero();
        assertThat(badLastNameErr.get(0).getValidationIssueCode()).isEqualTo("LEGALLASTNAMEBADVALUE");
    }

    @Test
    void testStudentUsualLastNameRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

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
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("0000000000000005");
        school.setSchoolReportingRequirementCode("RT");
        val validationCodeReportError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationCodeReportError.size()).isNotZero();
        val error1 = validationCodeReportError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDWRONGREPORTING"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes("4000000000000005");
        val noValidationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(noValidationError.size()).isZero();
    }

    @Test
    void testEnrolledProgramParseRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();

        entity.setEnrolledProgramCodes("143");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo("ENROLLEDCODEPARSEERR");

        entity.setEnrolledProgramCodes("0 4017293633");
        val validationErrorSpace = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorSpace.size()).isNotZero();
        val error1 = validationErrorSpace.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEPARSEERR"));
        assertThat(error1).isTrue();

    }

    @Test
    void testPenRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
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

        val dupePenCount = sdcSchoolCollectionStudentRepository.countForDuplicateStudentPENs(entity.getSdcSchoolCollectionID(), entity.getStudentPen());
        assertThat(dupePenCount).isEqualTo(2);

        entity.setStudentPen("523456789");
        val validationErrorDupe = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorDupe.size()).isNotZero();
        assertThat(validationErrorDupe.get(0).getValidationIssueCode()).isEqualTo("PENCHECKDIGITERR");

        entity.setStudentPen(null);
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        assertThat(validationError.get(0).getValidationIssueCode()).isEqualTo("STUDENTPENDUPLICATE");

        entity.setStudentPen("2345");
        val validationDigitError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationDigitError.size()).isNotZero();
        assertThat(validationDigitError.get(0).getValidationIssueCode()).isEqualTo("PENCHECKDIGITERR");
    }

    @Test
    void testHomeSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("HS");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val vError = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSLANG") && val.getValidationIssueSeverityCode().equals("WARNING"));
        assertThat(vError).isTrue();

        entity.setEnrolledProgramCodes("33");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSIND") && val.getValidationIssueSeverityCode().equals("WARNING"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("40");
        val validationErrorSped = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorSped.size()).isNotZero();
        val error1 = validationErrorSped.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSSPED"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes("33");
        entity.setCareerProgramCode("XA");
        val validationErrorCarProg = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCarProg.size()).isNotZero();
        val error2 = validationErrorCarProg.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSCAREER"));
        assertThat(error2).isTrue();

        entity.setEnrolledProgramCodes("40");
        entity.setSpecialEducationCategoryCode(null);
        val validationErrorCarr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCarr.size()).isNotZero();
        val error3 = validationErrorCarProg.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSCAREER") && val.getValidationIssueSeverityCode().equals("WARNING"));
        assertThat(error3).isTrue();

        entity.setEnrolledProgramCodes("40");
        entity.setSpecialEducationCategoryCode(null);
        val validationErrorCarrCodes = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCarrCodes.size()).isNotZero();
        val error4 = validationErrorCarProg.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEHSCAREER") && val.getValidationIssueSeverityCode().equals("WARNING"));
        assertThat(error4).isTrue();
    }

    @Test
    void testFundingCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setSchoolFundingCode(null);
        entity.setBandCode(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorNull.size()).isZero();

        entity.setSchoolFundingCode("05");
        val validationError5 = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError5.size()).isNotZero();
        assertThat(validationError5.get(0).getValidationIssueCode()).isEqualTo("FUNDINGCODEINVALID");
    }

    @Test
    void testIndigenousCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        entity.setSchoolFundingCode("20");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setSchoolFundingCode("14");
        val validationErrorCode = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCode.size()).isNotZero();
        val error1 = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("BANDCODEBLANK"));
        assertThat(error1).isTrue();

        entity.setSchoolFundingCode("20");
        entity.setBandCode(null);
        val validationErrorNull = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorNull.size()).isNotZero();
        val error2 = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("BANDCODEBLANK"));
        assertThat(error2).isTrue();

        entity.setSchoolFundingCode(null);
        entity.setBandCode("0500");
        val validationErrorFNull = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorFNull.size()).isNotZero();

        entity.setSchoolFundingCode("20");
        entity.setBandCode("0500");
        entity.setNativeAncestryInd("K");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        assertThat(validationErrorInd.get(0).getValidationIssueCode()).isEqualTo("NATIVEINDINVALID");

        entity.setSchoolFundingCode("20");
        entity.setBandCode("0000");
        entity.setNativeAncestryInd("Y");
        val validationErrorInvalid = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInvalid.size()).isNotZero();
        assertThat(validationErrorInvalid.get(0).getValidationIssueCode()).isEqualTo("BANDCODEINVALID");
    }

    @Test
    void testGradeCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("X");
        val validationErrorCode = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCode.size()).isNotZero();
        val error = validationErrorCode.stream().anyMatch(val -> val.getValidationIssueCode().equals("INVALIDGRADECODE"));
        assertThat(error).isTrue();

        entity.setEnrolledGradeCode(null);
        val validationErrorCodeNull = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCodeNull.size()).isNotZero();
        assertThat(validationErrorCodeNull.get(0).getValidationIssueCode()).isEqualTo("INVALIDGRADECODE");

    }

    @Test
    void testNonHSGradeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setEnrolledProgramCodes("0000000000000000");
        val validationCodeError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeError.size()).isNotZero();
        val error = validationCodeError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEINVALID"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("0800000000000005");
        val validationCodeErrorCount = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeErrorCount.size()).isNotZero();
        val errorCount = validationCodeErrorCount.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECOUNTERR"));
        assertThat(errorCount).isTrue();

        entity.setEnrolledProgramCodes("0000000000000014");
        entity.setEnrolledGradeCode("05");
        val validationCodeProgErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeProgErr.size()).isNotZero();
        val errorProg = validationCodeProgErr.stream().noneMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEFRANCOPHONEERR"));
        assertThat(errorProg).isTrue();

        entity.setEnrolledProgramCodes("0000000000000033");
        entity.setEnrolledGradeCode("06");
        entity.setNativeAncestryInd("N");
        val validationCodeIndErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeIndErr.size()).isNotZero();
        val errorInd = validationCodeIndErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("PROGRAMCODEIND"));
        assertThat(errorInd).isTrue();

        entity.setEnrolledProgramCodes("0000004000000041");
        val validationCodeCrrCount = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeCrrCount.size()).isNotZero();
        val errorCarrCount = validationCodeCrrCount.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODECOUNTERR"));
        assertThat(errorCarrCount).isTrue();

        entity.setEnrolledProgramCodes("0000000000000040");
        entity.setEnrolledGradeCode("01");
        val validationCrrGradeErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCrrGradeErr.size()).isNotZero();
        val errorCarrGradeErr = validationCrrGradeErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEGRADEERR"));
        assertThat(errorCarrGradeErr).isTrue();

        entity.setEnrolledProgramCodes("0000004000000040");
        entity.setCareerProgramCode(null);
        entity.setEnrolledGradeCode("08");
        val validationCodeCrrErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeCrrErr.size()).isNotZero();
        val errorCarrErr = validationCodeCrrErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREERCODEPROGERR"));
        assertThat(errorCarrErr).isTrue();

        entity.setEnrolledGradeCode("08");
        entity.setSpecialEducationCategoryCode("0");
        val validationCodeSpedErr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCodeSpedErr.size()).isNotZero();
        val errorSpedErr = validationCodeSpedErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("SPEDERR"));
        assertThat(errorSpedErr).isTrue();
    }

    @Test
    void testAdultStudentRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("DISTONLINE");

        entity.setDob("0210424F");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("19890101");
        entity.setNumberOfCourses("0000");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorOldDate.size()).isNotZero();
        val zeroCoursesInd = validationErrorOldDate.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTZEROCOURSES"));
        assertThat(zeroCoursesInd).isTrue();
//        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("ADULTZEROCOURSES");

        entity.setDob("19890101");
        entity.setNumberOfCourses("0100");
        entity.setEnrolledGradeCode("01");
        val validationErrorGrade = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorGrade.size()).isNotZero();
        val errorInd = validationErrorGrade.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTINCORRECTGRADE"));
        assertThat(errorInd).isTrue();

        entity.setDob("19890101");
        entity.setNumberOfCourses("0100");
        entity.setEnrolledGradeCode("01");
        val validationErrorOnlineGrade = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorOnlineGrade.size()).isNotZero();
        val errorOnlineInd = validationErrorOnlineGrade.stream().anyMatch(val -> val.getValidationIssueCode().equals("ADULTGRADEERR"));
        assertThat(errorOnlineInd).isTrue();
    }

    @Test
    void testHomeLanguageRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("08");
        entity.setHomeLanguageSpokenCode(null);
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInvalidDate.size()).isZero();

        entity.setHomeLanguageSpokenCode("00U");
        val validationErrorLang = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorLang.size()).isNotZero();
        assertThat(validationErrorLang.get(0).getValidationIssueCode()).isEqualTo("SPOKENLANGERR");
    }

    @Test
    void testSchoolAgedStudentRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("08");
        val school = createMockSchool();
        school.setFacilityTypeCode("STANDARD");

        entity.setDob("0210424F");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20180101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorOldDate.size()).isZero();
        //assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("AGELESSTHANFIVE");

        entity.setDob("20190101");
        school.setFacilityTypeCode("CONT_ED");
        val validationErrorContEd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorContEd.size()).isNotZero();
        val errorContEd = validationErrorContEd.stream().anyMatch(val -> val.getValidationIssueCode().equals("CONTEDERR"));
        assertThat(errorContEd).isTrue();

        entity.setDob("20150101");
        entity.setNumberOfCourses("0000");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("SCHOOLAGEZEROCOURSES"));
        assertThat(error).isTrue();
    }

    @Test
    void testNoOfCoursesRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setNumberOfCourses("0500");
        entity.setEnrolledGradeCode("08");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isZero();

        entity.setNumberOfCourses("1600");
        entity.setEnrolledGradeCode("08");
        val validationErrorMax = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorMax.size()).isNotZero();
        val errorContEd = validationErrorMax.stream().anyMatch(val -> val.getValidationIssueCode().equals("NOOFCOURSEMAX"));
        assertThat(errorContEd).isTrue();
    }

    @Test
    void testHSSchoolAgeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("HS");

        entity.setDob("20190101");
        val validationErrorAdult = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorAdult.size()).isNotZero();
        val errorContEd = validationErrorAdult.stream().anyMatch(val -> val.getValidationIssueCode().equals("HSNOTSCHOOLAGE"));
        assertThat(errorContEd).isTrue();

        entity.setDob("20160101");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("HSNOTSCHOOLAGE"));
        assertThat(error).isFalse();
    }

    @Test
    void testFundingCode14Rule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        entity.setEnrolledGradeCode("01");
        entity.setSchoolFundingCode("14");
        entity.setBandCode(null);

        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEFUNDINGERR"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("33");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODEINDERR"));
        assertThat(error1).isTrue();

        entity.setEnrolledProgramCodes("XA");
        val validationErrorCarrCodes = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCarrCodes.size()).isNotZero();
        val error2 = validationErrorCarrCodes.stream().noneMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECAREERERR"));
        assertThat(error2).isTrue();

        entity.setEnrolledProgramCodes("40");
        val validationErrorCarr = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorCarr.size()).isNotZero();
        val error3 = validationErrorCarr.stream().anyMatch(val -> val.getValidationIssueCode().equals("ENROLLEDCODECAREERERR"));
        assertThat(error3).isTrue();
    }

    @Test
    void testSummerSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setFacilityTypeCode("SUMMER");

        entity.setDob("19890101");
        val saga = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school);
        saga.setCollectionTypeCode("July");

        val validationErrorDOB = rulesProcessor.processRules(saga);
        assertThat(validationErrorDOB.size()).isNotZero();
        val error = validationErrorDOB.stream().anyMatch(val -> val.getValidationIssueCode().equals("STUDENTADULTERR"));
        assertThat(error).isTrue();

        entity.setDob("20150101");
        entity.setSupportBlocks("1");
        val saga2 = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school);
        saga2.setCollectionTypeCode("July");

        val validationErrorSupportBlock = rulesProcessor.processRules(saga2);
        assertThat(validationErrorSupportBlock.size()).isNotZero();
        val error1 = validationErrorSupportBlock.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUMMERSUPPORTBLOCKSNA"));
        assertThat(error1).isTrue();

        entity.setDob("20150101");
        entity.setSupportBlocks("0");
        entity.setEnrolledGradeCode("HS");
        val saga3 = createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school);
        saga3.setCollectionTypeCode("July");

        val validationErrorGradeErr = rulesProcessor.processRules(saga3);
        assertThat(validationErrorGradeErr.size()).isNotZero();
        val errorGrade = validationErrorGradeErr.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUMMERGRADECODE"));
        assertThat(errorGrade).isTrue();
    }

    @Test
    void testOffshoreSchoolRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
        val school = createMockSchool();
        school.setSchoolCategoryCode("OFFSHORE");

        entity.setSpecialEducationCategoryCode("05");
        val validationErrorDOB = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorDOB.size()).isNotZero();
        val error = validationErrorDOB.stream().noneMatch(val -> val.getValidationIssueCode().equals("SPEDOFFSHOREERR"));
        assertThat(error).isTrue();

        entity.setEnrolledProgramCodes("40");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("CAREEROFFSHOREERR"));
        assertThat(error1).isTrue();
    }

    @Test
    void testOtherCoursesRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setOtherCourses(null);
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("OTHERCOURSEINVALID"));
        assertThat(error).isFalse();

        entity.setOtherCourses("10");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("OTHERCOURSEINVALID"));
        assertThat(error1).isTrue();
    }

    @Test
    void testPostalCodeRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setPostalCode(null);
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("MISSINGPOSTALCODE"));
        assertThat(error).isTrue();

        val school = createMockSchool();
        school.setSchoolCategoryCode("OFFSHORE");
        entity.setPostalCode(null);
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().anyMatch(val -> val.getValidationIssueCode().equals("MISSINGPOSTALCODE"));
        assertThat(error1).isFalse();

        entity.setPostalCode("11111");
        val validationPattern = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationPattern.size()).isNotZero();
        val errorPatt = validationPattern.stream().anyMatch(val -> val.getValidationIssueCode().equals("INVALIDPOSTALCODE"));
        assertThat(errorPatt).isTrue();
    }

    @Test
    void testSupportBlockRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setSupportBlocks("9");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUPPORTBLOCKSINVALID"));
        assertThat(error).isTrue();

        val school = createMockSchool();
        school.setFacilityTypeCode("DISTONLINE");
        entity.setSupportBlocks("9");
        val validationErrorInd = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), school));
        assertThat(validationErrorInd.size()).isNotZero();
        val error1 = validationErrorInd.stream().noneMatch(val -> val.getValidationIssueCode().equals("SUPPORTFACILITYNA"));
        assertThat(error1).isTrue();

        entity.setDob("20160107");
        entity.setEnrolledGradeCode("GA");
        val validationDOB = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationDOB.size()).isNotZero();
        val errorDOB = validationDOB.stream().noneMatch(val -> val.getValidationIssueCode().equals("GAERROR"));
        assertThat(errorDOB).isTrue();

        entity.setNumberOfCourses(null);
        val validationCourses = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationCourses.size()).isNotZero();
        val errorCourses = validationCourses.stream().anyMatch(val -> val.getValidationIssueCode().equals("SUPPORTBLOCKSNOTCOUNT"));
        assertThat(errorCourses).isFalse();
    }

    @Test
    void testKHRule() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setEnrolledGradeCode("KH");
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationError.size()).isNotZero();
        val error = validationError.stream().noneMatch(val -> val.getValidationIssueCode().equals("KHGRADECODEINVALID"));
        assertThat(error).isTrue();
    }

}
