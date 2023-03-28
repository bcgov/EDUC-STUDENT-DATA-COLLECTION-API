package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentCount;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class SdcFileController implements SdcFileEndpoint {

  private final SdcFileService sdcFileService;

  private final SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator;

  public SdcFileController(SdcFileService sdcFileService, SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator) {
    this.sdcFileService = sdcFileService;
    this.sdcSchoolCollectionFileValidator = sdcSchoolCollectionFileValidator;
  }

  @Override
  public ResponseEntity<SdcSchoolCollection> processSdcBatchFile(SdcFileUpload fileUpload, String correlationID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionFileValidator.validatePayload(fileUpload));
    var sdcSchoolCollection = sdcFileService.runFileLoad(fileUpload);
    return ResponseEntity.ok(sdcSchoolCollection);
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
