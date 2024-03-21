package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileSummary;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import ca.bc.gov.educ.studentdatacollection.api.validator.SdcSchoolCollectionFileValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
public class SdcFileController implements SdcFileEndpoint {

  private final SdcFileService sdcFileService;

  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  private final SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator;

  public SdcFileController(SdcFileService sdcFileService, SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionFileValidator sdcSchoolCollectionFileValidator) {
    this.sdcFileService = sdcFileService;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionFileValidator = sdcSchoolCollectionFileValidator;
  }

  @Override
  public ResponseEntity<SdcSchoolCollection> processSdcBatchFile(SdcFileUpload fileUpload, String sdcSchoolCollectionID, String correlationID) {
    ValidationUtil.validatePayload(() -> this.sdcSchoolCollectionFileValidator.validatePayload(sdcSchoolCollectionID));
    log.info("Running file load for file: " + fileUpload.getFileName());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcFileService.runFileLoad(fileUpload, sdcSchoolCollectionID);
    log.info("File data committed for file: " + fileUpload.getFileName());
    var mapped = SdcSchoolCollectionMapper.mapper.toStructure(sdcSchoolCollectionEntity);
    log.info("Mapping data complete");
    return ResponseEntity.ok(mapped);
  }

  @Override
  public ResponseEntity<SdcFileSummary> isBeingProcessed(String sdcSchoolCollectionID) {
    return ResponseEntity.ok(sdcSchoolCollectionService.isSdcSchoolCollectionBeingProcessed(UUID.fromString(sdcSchoolCollectionID)));
  }


}
