package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchFileProcessor;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity runFileLoad(SdcFileUpload sdcFileUpload){
    log.debug("Uploaded file contents for school collection ID: {}", sdcFileUpload.getSdcSchoolCollectionID());
    return this.getSdcBatchProcessor().processSdcBatchFile(sdcFileUpload);
  }
}
