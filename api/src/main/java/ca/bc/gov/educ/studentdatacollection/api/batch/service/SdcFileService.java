package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchFileProcessor;
import ca.bc.gov.educ.studentdatacollection.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.StaleStateException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcFileService {

  @Getter(PRIVATE)
  private final SdcBatchFileProcessor sdcBatchProcessor;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity runFileLoad(SdcFileUpload sdcFileUpload, String sdcSchoolCollectionID) {
    log.info("Uploaded file contents for school collection ID: {}", sdcSchoolCollectionID);
    return this.getSdcBatchProcessor().processSdcBatchFile(sdcFileUpload, sdcSchoolCollectionID);
  }

  @Retryable(retryFor = {StaleStateException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionEntity runDistrictFileLoad(SdcFileUpload sdcFileUpload, String sdcDistrictCollectionID) {
    if (RetrySynchronizationManager.getContext() != null && RetrySynchronizationManager.getContext().getRetryCount() > 0) {
      log.info("Retrying SDC file load after StaleStateException, sdcDistrictCollectionID: {}", sdcDistrictCollectionID);
    }
    log.info("Uploaded file contents for district collection ID: {}", sdcDistrictCollectionID);
    return this.getSdcBatchProcessor().processDistrictSdcBatchFile(sdcFileUpload, sdcDistrictCollectionID);
  }

}
