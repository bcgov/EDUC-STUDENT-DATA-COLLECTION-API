package ca.bc.gov.educ.studentdatacollection.api.batch.processor;


import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.studentdatacollection.api.batch.mappers.SdcFileMapper;
import ca.bc.gov.educ.studentdatacollection.api.batch.mappers.StringMapper;
import ca.bc.gov.educ.studentdatacollection.api.batch.service.SdcFileService;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileHeader;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileTrailer;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.batch.validator.SdcFileValidator;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.mozilla.universalchardet.UniversalDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileError.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.BatchFileConstants.*;
import static lombok.AccessLevel.PRIVATE;

/**
 * The Pen reg batch processor.
 *
 * @author OM
 */
@Component
@Slf4j
public class SdcBatchProcessor {

  /**
   * The constant mapper.
   */
  private static final SdcFileMapper mapper = SdcFileMapper.mapper;
  @Getter(PRIVATE)
  private final SdcBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor;
  public static final String TRANSACTION_CODE_STUDENT_DETAILS_RECORD = "SRM";

  @Getter(PRIVATE)
  private final SdcFileService sdcFileService;

  @Getter
  private final ApplicationProperties applicationProperties;

  @Getter(PRIVATE)
  private final RestUtils restUtils;

  @Getter(PRIVATE)
  private final SdcFileValidator sdcFileValidator;

  @Autowired
  public SdcBatchProcessor(final SdcBatchStudentRecordsProcessor penRegBatchStudentRecordsProcessor, final SdcFileService sdcFileService, final ApplicationProperties applicationProperties, final RestUtils restUtils, SdcFileValidator sdcFileValidator) {
    this.penRegBatchStudentRecordsProcessor = penRegBatchStudentRecordsProcessor;
    this.sdcFileService = sdcFileService;
    this.applicationProperties = applicationProperties;
    this.sdcFileValidator = sdcFileValidator;
    this.restUtils = restUtils;
  }

  /**
   * Process pen reg batch file from tsw.
   * 1. <p>The data comes from TSW table so if the the data from the TSW table cant be read error is logged and email is sent.</p>
   * 2. <p>If The data is successfully retrieved from TSW table and file header cant be parsed, system will create only the header record and persist it.
   *
   * @param penWebBlob the pen web blob entity
   */
  @Transactional
  @Async("sdcFileProcessor")
  public void processSdcBatchFile(@NonNull final SdcFileUpload fileUpload) {
    val stopwatch = Stopwatch.createStarted();
    final var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing SDC file with school ID :: {} and guid :: {}", fileUpload.getSchoolID(), guid);
    val batchFile = new SdcBatchFile();
    Optional<Reader> batchFileReaderOptional = Optional.empty();
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      var byteArrayOutputStream = new ByteArrayInputStream(fileUpload.getFileContents().getBytes());
      var encoding = UniversalDetector.detectCharset(byteArrayOutputStream);
      byteArrayOutputStream.reset();
      if(!StringUtils.isEmpty(encoding) && !encoding.equals("UTF-8")){
        encoding = "windows-1252";
      }

      if(StringUtils.isEmpty(encoding)){
        batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
      }else{
        batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream, Charset.forName(encoding).newDecoder()));
      }
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      this.sdcFileValidator.validateFileForFormatAndLength(guid, ds);
      this.populateBatchFile(guid, ds, batchFile);
      this.sdcFileValidator.validateStudentCountForMismatchAndSize(guid, batchFile);
      this.processLoadedRecordsInBatchFile(guid, batchFile, penWebBlobEntity);
    } catch (final FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      this.processFileUnProcessableException(guid, penWebBlobEntity, fileUnProcessableException, batchFile);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
    } finally {
      batchFileReaderOptional.ifPresent(this::closeBatchFileReader);
      stopwatch.stop();
      log.info("Time taken for batch processed is :: {} milli seconds", stopwatch.elapsed(TimeUnit.MILLISECONDS));
    }
  }

  /**
   * Close batch file reader.
   *
   * @param reader the reader
   */
  private void closeBatchFileReader(final Reader reader) {
    try {
      if (reader != null) {
        reader.close();
      }
    } catch (final IOException e) {
      log.warn("Error closing the batch file :: ", e);
    }
  }

  /**
   * Persist data with exception.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   * @param fileUnProcessableException the file un processable exception
   * @param batchFile                  the batch file
   */
  private void processFileUnProcessableException(@NonNull final String guid, @NonNull final PENWebBlobEntity penWebBlobEntity, @NonNull final FileUnProcessableException fileUnProcessableException, final BatchFile batchFile) {
    val notifySchoolForFileFormatErrorsOptional = this.notifySchoolForFileFormatErrors(guid, penWebBlobEntity, fileUnProcessableException);
    final PenRequestBatchEntity entity = mapper.toPenReqBatchEntityForBusinessException(penWebBlobEntity, fileUnProcessableException.getReason(), fileUnProcessableException.getPenRequestBatchStatusCode(), batchFile, persistStudentRecords(fileUnProcessableException.getFileError())); // batch file can be processed further and persisted.
    final Optional<School> school = this.restUtils.getSchoolByMincode(penWebBlobEntity.getMincode());
    school.ifPresent(value -> entity.setSchoolName(value.getSchoolName()));
    //wait here if notification was sent, if there was any error this file will be picked up again as it wont be persisted.
    if (notifySchoolForFileFormatErrorsOptional.isPresent()) {
      final boolean isNotified = this.waitForNotificationToCompleteIfPresent(guid, notifySchoolForFileFormatErrorsOptional.get());
      if (isNotified) {
        log.info("going to persist data with FileUnProcessableException for batch :: {}", guid);
        this.getSdcFileService().markInitialLoadComplete(entity, penWebBlobEntity);
      } else {
        log.warn("Batch file could not be persisted as system was not able to send required notification to school, it will be picked up again by the scheduler.");
      }
    } else {
      log.info("going to persist data with FileUnProcessableException for batch :: {}", guid);
      this.getSdcFileService().markInitialLoadComplete(entity, penWebBlobEntity);
    }
  }
  private boolean persistStudentRecords(FileError fileError) {
    return fileError == HELD_BACK_FOR_SIZE || fileError == HELD_BACK_FOR_SFAS;
  }

  /**
   * Wait for notification to complete if present.
   *
   * @param guid                     the guid
   * @param booleanCompletableFuture the boolean completable future
   * @return the boolean
   */
  private boolean waitForNotificationToCompleteIfPresent(final String guid, final CompletableFuture<Boolean> booleanCompletableFuture) {
    try {
      final boolean isNotificationSuccessful = booleanCompletableFuture.get(2000L, TimeUnit.MILLISECONDS); // wait here for result.
      log.info("notification result for :: {} is :: {}", guid, isNotificationSuccessful);
      return isNotificationSuccessful;
    } catch (final InterruptedException e) {
      log.error("InterruptedException ", e);
      Thread.currentThread().interrupt();
    } catch (final ExecutionException | TimeoutException e) {
      log.error("ExecutionException | TimeoutException ", e);
    }
    return false;
  }

  /**
   * Notify school for file format errors optional.
   *
   * @param guid                       the guid
   * @param penWebBlobEntity           the pen web blob entity
   * @param fileUnProcessableException the file un processable exception
   * @return the optional
   */
  private Optional<CompletableFuture<Boolean>> notifySchoolForFileFormatErrors(final String guid, final PENWebBlobEntity penWebBlobEntity, final FileUnProcessableException fileUnProcessableException) {
    Optional<CompletableFuture<Boolean>> isSchoolNotifiedFutureOptional = Optional.empty();
    if (this.isNotificationToSchoolRequired(fileUnProcessableException)) {
      log.info("notification to school is required :: {}", guid);
      val coordinatorEmailOptional = this.penCoordinatorService.getPenCoordinatorEmailByMinCode(penWebBlobEntity.getMincode());
      if (coordinatorEmailOptional.isPresent()) {
        log.info("pen coordinator email found :: {}, for guid :: {}", coordinatorEmailOptional.get(), guid);
        isSchoolNotifiedFutureOptional = Optional.ofNullable(this.notificationService.notifySchoolForLoadFailed(guid, penWebBlobEntity.getFileName(), penWebBlobEntity.getSubmissionNumber(), fileUnProcessableException.getReason(), coordinatorEmailOptional.get()));
      }
    }
    return isSchoolNotifiedFutureOptional;
  }

  /**
   * Is notification to school required boolean. notify school in all other conditions than the below ones.
   *
   * @param fileUnProcessableException the file un processable exception
   * @return the boolean
   */
  private boolean isNotificationToSchoolRequired(final FileUnProcessableException fileUnProcessableException) {
    return fileUnProcessableException.getFileError() != INVALID_MINCODE_SCHOOL_CLOSED
      && fileUnProcessableException.getFileError() != INVALID_MINCODE_HEADER
      && fileUnProcessableException.getFileError() != DUPLICATE_BATCH_FILE_PSI
      && fileUnProcessableException.getFileError() != HELD_BACK_FOR_SIZE
      && fileUnProcessableException.getFileError() != HELD_BACK_FOR_SFAS;
  }


  /**
   * System was able to process the file successfully, now the data is persisted and saga data is created for further processing.
   * Process loaded records in batch file set.
   * this method will convert from batch file to header and student record,
   * send them to service for persistence and then return the set for further processing.
   *
   * @param guid             the guid
   * @param batchFile        the batch file
   * @param penWebBlobEntity the pen web blob entity
   */
  private void processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final SdcBatchFile batchFile) {
    var counter = 1;
    log.info("going to persist data for batch :: {}", guid);
    final SdcSchoolBatchEntity entity = mapper.toSdcBatchEntityLoaded(batchFile); // batch file can be processed further and persisted.
    final Optional<School> school = this.restUtils.getSchoolByMincode(penWebBlobEntity.getMincode());
    school.ifPresent(value -> entity.setSchoolName(value.getSchoolName()));
    for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      final var penRequestBatchStudentEntity = mapper.toPenRequestBatchStudentEntity(student, entity);
      penRequestBatchStudentEntity.setRecordNumber(counter++);
      entity.getPenRequestBatchStudentEntities().add(penRequestBatchStudentEntity);
    }
    this.getSdcFileService().markInitialLoadComplete(entity, penWebBlobEntity);
    if (entity.getPenRequestBatchID() != null) { // this could happen when the same submission number is picked up again, system should not process the same submission.
      // the entity was saved in propagation new context , so system needs to get it again from DB to have an attached entity bound to the current thread.
      this.getSdcFileService().filterDuplicatesAndRepeatRequests(guid, entity);
    }
  }


  /**
   * Populate batch file.
   *
   * @param guid      the guid
   * @param ds        the ds
   * @param batchFile the batch file
   * @throws FileUnProcessableException the file un processable exception
   */
  public void populateBatchFile(final String guid, final DataSet ds, final SdcBatchFile batchFile) throws FileUnProcessableException {
    long index = 0;
    while (ds.next()) {
      if (ds.isRecordID(HEADER.getName()) || ds.isRecordID(TRAILER.getName())) {
        this.setHeaderOrTrailer(ds, batchFile);
        index++;
        continue;
      }
      batchFile.getStudentDetails().add(this.getStudentDetailRecordFromFile(ds, guid, index));
      index++;
    }

    if(batchFile.getBatchFileTrailer() == null) {
      setManualTrailer(guid, ds, batchFile);
    }
  }

  private void setManualTrailer(final String guid, final DataSet ds, final SdcBatchFile batchFile) throws FileUnProcessableException {
    String rawTrailer = ds.getErrors().get(ds.getErrors().size()-1).getRawData();

    if(rawTrailer == null || rawTrailer.length() < 9){
      throw new FileUnProcessableException(INVALID_TRAILER, guid, SdcBatchStatusCodes.LOAD_FAIL);
    }
    String studentCount = rawTrailer.substring(3,9).trim();
    if(!StringUtils.isNumeric(studentCount)){
      throw new FileUnProcessableException(INVALID_TRAILER_STUDENT_COUNT, guid, SdcBatchStatusCodes.LOAD_FAIL);
    }

    var trailer = new SdcBatchFileTrailer();
    trailer.setStudentCount(studentCount);
    batchFile.setBatchFileTrailer(trailer);
  }

  /**
   * Gets student detail record from file.
   *
   * @param ds    the ds
   * @param guid  the guid
   * @param index the index
   * @return the student detail record from file
   * @throws FileUnProcessableException the file un processable exception
   */
  private StudentDetails getStudentDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws FileUnProcessableException {
    final var transactionCode = ds.getString(TRANSACTION_CODE.getName());
    if (!TRANSACTION_CODE_STUDENT_DETAILS_RECORD.equals(transactionCode)) {
      throw new FileUnProcessableException(INVALID_TRANSACTION_CODE_STUDENT_DETAILS, guid, SdcBatchStatusCodes.LOAD_FAIL, String.valueOf(index), ds.getString(LOCAL_STUDENT_ID.getName()));
    }
    return StudentDetails.builder()
      .birthDate(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(BIRTH_DATE.getName())))
      .enrolledGradeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(ENROLLED_GRADE_CODE.getName())))
      .gender(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(GENDER.getName())))
      .legalGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_GIVEN_NAME.getName())))
      .legalMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_MIDDLE_NAME.getName())))
      .legalSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_SURNAME.getName())))
      .localStudentID(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LOCAL_STUDENT_ID.getName())))
      .pen(ds.getString(PEN.getName()))
      .postalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(POSTAL_CODE.getName())))
      .transactionCode(transactionCode)
      .unusedBlock1(ds.getString(UNUSED.getName()))
      .unusedSecond(ds.getString(UNUSED_SECOND.getName()))
      .usualGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_GIVEN_NAME.getName())))
      .usualMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_MIDDLE_NAME.getName())))
      .usualSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_SURNAME.getName())))
      .build();
  }

  /**
   * Sets header or trailer.
   *
   * @param ds        the ds
   * @param batchFile the batch file
   */
  private void setHeaderOrTrailer(final DataSet ds, final SdcBatchFile batchFile) {
    if (ds.isRecordID(HEADER.getName())) {
      //Just set transactionCode because of different flavours of header
      batchFile.setBatchFileHeader(SdcBatchFileHeader.builder()
        .transactionCode(ds.getString(TRANSACTION_CODE.getName()))
        .build());
    } else if (ds.isRecordID(TRAILER.getName())) {
      final var transactionCode = ds.getString(TRANSACTION_CODE.getName());
      batchFile.setBatchFileTrailer(SdcBatchFileTrailer.builder()
        .transactionCode(transactionCode)
        .productID(ds.getString(PRODUCT_ID.getName()))
        .productName(ds.getString(PRODUCT_NAME.getName()))
        .studentCount(ds.getString(STUDENT_COUNT.getName()))
        .vendorName(ds.getString(VENDOR_NAME.getName()))
        .build());
    }
  }


}
