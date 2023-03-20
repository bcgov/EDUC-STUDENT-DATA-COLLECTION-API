package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchProcessor;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch file service.
 *
 * @author OM
 */
@Service
@Slf4j
public class SdcFileService {

  @Getter(PRIVATE)
  private final SdcBatchProcessor sdcBatchProcessor;

  @Autowired
  public SdcFileService(SdcBatchProcessor sdcBatchProcessor) {
    this.sdcBatchProcessor = sdcBatchProcessor;
  }

  public void runFileLoad(SdcFileUpload sdcFileUpload){
    log.debug("Uploaded file contents for school ID {} :: {}", sdcFileUpload.getSchoolID(),sdcFileUpload.getFileContents());
    this.getSdcBatchProcessor().processSdcBatchFile(sdcFileUpload);
  }
}
