package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.UUID;

@RestController
@Slf4j
public class SdcFileController implements SdcFileEndpoint {

  private final SdcFileService sdcFileService;

  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  private final SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator;

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  public SdcFileController(
    SdcFileService sdcFileService,
    SdcSchoolCollectionService sdcSchoolCollectionService,
    SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator,
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository
  ) {
    this.sdcFileService = sdcFileService;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionFileValidator = sdcSchoolCollectionFileValidator;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
  }

  @Override
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public ResponseEntity<SdcSchoolCollection> processSdcBatchFile(SdcFileUpload fileUpload, String sdcSchoolCollectionID, String correlationID) {
    Optional<SdcSchoolCollectionEntity> schoolCollectionEntity = this.sdcSchoolCollectionRepository
      .findById(UUID.fromString(sdcSchoolCollectionID));

    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionFileValidator.
      validatePayload(sdcSchoolCollectionID, schoolCollectionEntity));

    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcFileService.runFileLoad(
      fileUpload,
      sdcSchoolCollectionID,
      schoolCollectionEntity
    );

    return ResponseEntity.ok(SdcSchoolCollectionMapper.mapper.toSdcSchoolWithStudents(sdcSchoolCollectionEntity));
  }

  @Override
  public ResponseEntity<SdcFileSummary> isBeingProcessed(String sdcSchoolCollectionID) {
    return ResponseEntity.ok(sdcSchoolCollectionService.isSdcSchoolCollectionBeingProcessed(UUID.fromString(sdcSchoolCollectionID)));
  }


}
