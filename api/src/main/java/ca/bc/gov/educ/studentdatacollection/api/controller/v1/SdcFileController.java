package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcFileEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
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
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchFileProcessor.INVALID_PAYLOAD_MSG;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
    return ResponseEntity.ok(mapped);
  }

  @Override
  public ResponseEntity<SdcFileSummary> isBeingProcessed(String sdcSchoolCollectionID) {
    return ResponseEntity.ok(sdcSchoolCollectionService.getSummarySdcSchoolCollectionBeingProcessed(UUID.fromString(sdcSchoolCollectionID)));
  }

  @Override
  public ResponseEntity<SdcSchoolCollection> processDistrictSdcBatchFile(SdcFileUpload fileUpload, String sdcDistrictCollectionID, String correlationID) {
    try {
    log.info("Running file load for file: " + fileUpload.getFileName());
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcFileService.runDistrictFileLoad(fileUpload, sdcDistrictCollectionID);
    log.info("File data committed for file: " + fileUpload.getFileName());
    var mapped = SdcSchoolCollectionMapper.mapper.toStructure(sdcSchoolCollectionEntity);
    return ResponseEntity.ok(mapped);
  }
    catch (final ObjectOptimisticLockingFailureException objectOptimisticLockingFailureException) {
      log.error("Unable to persist records :: {}", objectOptimisticLockingFailureException);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError("sdcFileUpload", sdcDistrictCollectionID, "Unable to upload file. Please try again later");
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
    }
  }


}
