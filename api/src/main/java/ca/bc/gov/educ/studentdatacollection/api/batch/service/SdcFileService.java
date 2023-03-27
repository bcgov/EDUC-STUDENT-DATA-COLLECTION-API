package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchFileProcessor;
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
  private final SdcBatchFileProcessor sdcBatchProcessor;

  @Autowired
  public SdcFileService(SdcBatchFileProcessor sdcBatchProcessor) {
    this.sdcBatchProcessor = sdcBatchProcessor;
  }

  public void runFileLoad(SdcFileUpload sdcFileUpload){
    log.debug("Uploaded file contents for school collection ID: {}", sdcFileUpload.getSdcSchoolCollectionID());
    this.getSdcBatchProcessor().processSdcBatchFile(sdcFileUpload);
  }
}
