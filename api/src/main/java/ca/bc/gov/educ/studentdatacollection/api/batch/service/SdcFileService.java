package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchFileProcessor;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcFileService {

  @Getter(PRIVATE)
  private final SdcBatchFileProcessor sdcBatchProcessor;

  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity runFileLoad(
    SdcFileUpload sdcFileUpload,
    String sdcSchoolCollectionID,
    Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionOptional
  ) {
    log.debug("Uploaded file contents for school collection ID: {}", sdcSchoolCollectionID);

    if (sdcSchoolCollectionOptional.isPresent() &&
        StringUtils.isNotEmpty(sdcSchoolCollectionOptional.get().getUploadFileName())) {
      SdcSchoolCollectionEntity sdcSchoolCollection = sdcSchoolCollectionOptional.get();
      sdcSchoolCollection.setUploadFileName(null);
      sdcSchoolCollection.setUploadDate(null);
      sdcSchoolCollection.setUploadReportDate(null);
      sdcSchoolCollection.getSDCSchoolStudentEntities().clear();
      sdcSchoolCollectionService.saveSdcSchoolCollection(sdcSchoolCollection);
    }

    return this.getSdcBatchProcessor().processSdcBatchFile(
      sdcFileUpload,
      sdcSchoolCollectionID,
      sdcSchoolCollectionOptional
    );
  }
}
