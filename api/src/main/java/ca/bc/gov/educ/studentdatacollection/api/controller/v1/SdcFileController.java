package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentCount;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Slf4j
public class SdcFileController implements SdcFileEndpoint {

  private final SdcFileService sdcFileService;

  public SdcFileController(SdcFileService sdcFileService) {
    this.sdcFileService = sdcFileService;
  }

  @Override
  public ResponseEntity<Void> processSdcBatchFile(SdcFileUpload fileUpload, String correlationID) {
    sdcFileService.runFileLoad(fileUpload);
    return ResponseEntity.status(HttpStatus.CREATED).build();
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
