package ca.bc.gov.educ.studentdatacollection.api.rules;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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
    }

    @Test
    void testDOBRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setDob("19993001");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        entity.setDob(format.format(LocalDate.now().plusDays(2)));
        val validationErrorFutureDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorFutureDate.size()).isNotZero();
        assertThat(validationErrorFutureDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("18990101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity), createMockSchool()));
        assertThat(validationErrorOldDate.size()).isNotZero();
        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

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
        var collection = createMockCollectionEntity();
        var sdcSchoolCollectionEntity = createMockSdcSchoolCollectionEntity(collection, null);
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);
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
}
