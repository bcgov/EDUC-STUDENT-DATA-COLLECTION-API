package ca.bc.gov.educ.studentdatacollection.api.batch.processor;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolBatchRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
class SdcBatchProcessorTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private SdcBatchProcessor sdcBatchProcessor;
  @Autowired
  private SdcRepository sdcRepository;
  @Autowired
  private SdcSchoolBatchRepository sdcSchoolBatchRepository;
  @Autowired
  private SdcSchoolStudentRepository sdcSchoolStudentRepository;
  @Autowired
  RestUtils restUtils;

  @Test
  @Transactional
  void testProcessSdcBatchFileFromTSW_Given1RowValidFile_ShouldCreateRecordsInDB() throws IOException {
    var collection = sdcRepository.save(createCollectionEntity());
    var school = this.createMockSchool();
    when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
    final FileInputStream fis = new FileInputStream("src/test/resources/sample-1-student.txt");
    final String fileContents = Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
    var fileUpload = SdcFileUpload.builder().schoolID(school.getSchoolId()).fileContents(fileContents).collectionID(collection.getCollectionID().toString()).fileName("SampleUpload.txt").build();
    this.sdcBatchProcessor.processSdcBatchFile(fileUpload);
    final var result = this.sdcSchoolBatchRepository.findAll();
    assertThat(result).hasSize(1);
    final var entity = result.get(0);
    assertThat(entity.getSdcSchoolBatchID()).isNotNull();
    assertThat(entity.getStatusCode()).isEqualTo(SdcBatchStatusCodes.LOADED.getCode());
    final var students = this.sdcSchoolStudentRepository.findAllBySdcSchoolBatchEntity(result.get(0));
    assertThat(students).isNotNull();
  }

  private SdcEntity createCollectionEntity(){
    SdcEntity sdcEntity = new SdcEntity();
    sdcEntity.setCollectionCode("SEPTEMBER");
    sdcEntity.setOpenDate(LocalDateTime.now());
    sdcEntity.setCloseDate(null);
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  private School createMockSchool() {
    final School school = new School();
    school.setSchoolId(UUID.randomUUID().toString());
    school.setDisplayName("Marco's school");
    school.setMincode("66510518");
    school.setOpenedDate("1964-09-01T00:00:00");
    return school;
  }

}

