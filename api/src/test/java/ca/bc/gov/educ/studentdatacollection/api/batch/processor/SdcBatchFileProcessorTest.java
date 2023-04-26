package ca.bc.gov.educ.studentdatacollection.api.batch.processor;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
class SdcBatchFileProcessorTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private SdcBatchFileProcessor sdcBatchProcessor;
  @Autowired
  private CollectionRepository sdcRepository;
  @Autowired
  private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  @Autowired
  private SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;
  @Autowired
  RestUtils restUtils;

  @Test
  @Transactional
  void testProcessSdcBatchFileFromTSW_Given1RowValidFile_ShouldCreateRecordsInDB() throws IOException {
    var collection = sdcRepository.save(createMockCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    var sdcSchoolCollection = sdcSchoolCollectionRepository.save(createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId())));
    Optional<SdcSchoolCollectionEntity> schoolCollectionOptional = Optional.of(sdcSchoolCollection);
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    var fileUpload = SdcFileUpload.builder().fileContents(fileContents).fileName("SampleUpload.std").build();

    var response = this.sdcBatchProcessor.processSdcBatchFile(
      fileUpload,
      sdcSchoolCollection.getSdcSchoolCollectionID().toString(),
      schoolCollectionOptional
    );
    assertThat(response).isNotNull();

    final var result = this.sdcSchoolCollectionRepository.findAll();
    assertThat(result).hasSize(1);

    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolCollectionID()).isNotNull();
    assertThat(entity.getSdcSchoolCollectionStatusCode()).isEqualTo("NEW");

    final var students = this.sdcSchoolStudentRepository.findAllBySdcSchoolCollectionID(result.get(0).getSdcSchoolCollectionID());
    assertThat(students).isNotNull();
  }

}

