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
public class RulesProcessorTest extends BaseStudentDataCollectionAPITest {

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
        val validationError = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationError.size()).isEqualTo(0);

        entity.setGender("U");
        val validationErrorIncorrectVal = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationErrorIncorrectVal.size()).isNotZero();
        assertThat(validationErrorIncorrectVal.get(0).getValidationIssueFieldCode()).isEqualTo("GENDER_CODE");
    }

    @Test
    void testDOBRules() {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, null));
        val entity = this.createMockSchoolStudentEntity(sdcSchoolCollectionEntity);

        entity.setDob("19993001");
        val validationErrorInvalidDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationErrorInvalidDate.size()).isNotZero();
        assertThat(validationErrorInvalidDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        entity.setDob(format.format(LocalDate.now().plusDays(2)));
        val validationErrorFutureDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationErrorFutureDate.size()).isNotZero();
        assertThat(validationErrorFutureDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("18990101");
        val validationErrorOldDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationErrorOldDate.size()).isNotZero();
        assertThat(validationErrorOldDate.get(0).getValidationIssueCode()).isEqualTo("DOBINVALIDFORMAT");

        entity.setDob("20180420");
        val validationErrorCorrectDate = rulesProcessor.processRules(createMockStudentSagaData(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(entity)));
        assertThat(validationErrorCorrectDate.size()).isZero();

    }
}
