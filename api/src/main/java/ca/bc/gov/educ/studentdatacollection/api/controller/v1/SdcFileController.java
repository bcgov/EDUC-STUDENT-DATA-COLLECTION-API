package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentCount;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@Slf4j
public class SdcFileController implements SdcFileEndpoint {

  private final SdcFileService sdcFileService;

  private final SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator;

  private final SdcService sdcService;

  public SdcFileController(SdcFileService sdcFileService, SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator, SdcService sdcService) {
    this.sdcFileService = sdcFileService;
    this.sdcSchoolCollectionFileValidator = sdcSchoolCollectionFileValidator;
    this.sdcService = sdcService;
  }

  @Override
  public ResponseEntity<SdcSchoolCollection> processSdcBatchFile(SdcFileUpload fileUpload, String correlationID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionFileValidator.validatePayload(fileUpload));
    var sdcSchoolCollectionEntity = sdcFileService.runFileLoad(fileUpload);
    List<SdcSchoolCollectionStudentEntity> mainStudentList = new ArrayList<>();
    mainStudentList.addAll(sdcSchoolCollectionEntity.getSDCSchoolStudentEntities());
    this.sdcService.prepareAndSendSdcStudentsForFurtherProcessing(mainStudentList);
    return ResponseEntity.ok(SdcSchoolCollectionMapper.mapper.toSdcSchoolBatch(sdcSchoolCollectionEntity));
  }

  @Override
  public ResponseEntity<List<SdcStudentCount>> isBeingProcessed(String schoolID) {
    return null;
  }

  @Override
  public ResponseEntity<Void> deleteAll(String schoolID) {
    return null;
  }
}
