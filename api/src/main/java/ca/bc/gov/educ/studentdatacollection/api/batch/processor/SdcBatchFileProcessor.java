package ca.bc.gov.educ.studentdatacollection.api.batch.processor;


import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.studentdatacollection.api.batch.mappers.SdcBatchFileMapper;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileHeader;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileTrailer;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcStudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.batch.validator.SdcFileValidator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.StringMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.DefaultParserFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.FieldError;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileError.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchFileConstants.*;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

/**
 * The Pen reg batch processor.
 *
 * @author OM
 */
@Component
@Slf4j
public class SdcBatchFileProcessor {

  /**
   * The constant mapper.
   */
  private static final SdcBatchFileMapper mapper = SdcBatchFileMapper.mapper;
  @Getter(PRIVATE)
  private final SdcBatchFileStudentRecordsProcessor sdcBatchStudentRecordsProcessor;
  public static final String TRANSACTION_CODE_STUDENT_DETAILS_RECORD = "SRM";

  @Getter
  private final ApplicationProperties applicationProperties;

  @Getter(PRIVATE)
  private final RestUtils restUtils;

  @Getter(PRIVATE)
  private final SdcFileValidator sdcFileValidator;

  @Getter(PRIVATE)
  private final SdcSchoolCollectionService sdcSchoolCollectionService;


  @Getter(PRIVATE)
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  @Getter(PRIVATE)
  private final CollectionRepository sdcRepository;

  @Autowired
  public SdcBatchFileProcessor(final SdcBatchFileStudentRecordsProcessor sdcBatchStudentRecordsProcessor, final ApplicationProperties applicationProperties, final RestUtils restUtils, SdcFileValidator sdcFileValidator, SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, CollectionRepository sdcRepository) {
    this.sdcBatchStudentRecordsProcessor = sdcBatchStudentRecordsProcessor;
    this.applicationProperties = applicationProperties;
    this.sdcFileValidator = sdcFileValidator;
    this.restUtils = restUtils;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcRepository = sdcRepository;
  }

  /**
   * Process pen reg batch file from tsw.
   * 1. <p>The data comes from TSW table so if the the data from the TSW table cant be read error is logged and email is sent.</p>
   * 2. <p>If The data is successfully retrieved from TSW table and file header cant be parsed, system will create only the header record and persist it.
   *
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity processSdcBatchFile(@NonNull final SdcFileUpload fileUpload, String sdcSchoolCollectionID) {
    val stopwatch = Stopwatch.createStarted();
    final var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing SDC file with school collection ID :: {} and correlation guid :: {}", sdcSchoolCollectionID, guid);
    val batchFile = new SdcBatchFile();
    Optional<Reader> batchFileReaderOptional = Optional.empty();
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {
      var byteArrayOutputStream = new ByteArrayInputStream(Base64.getDecoder().decode(fileUpload.getFileContents()));
      batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();
      this.sdcFileValidator.validateFileForFormatAndLength(guid, ds);
      this.populateBatchFile(guid, ds, batchFile);
      this.sdcFileValidator.validateStudentCountForMismatchAndSize(guid, batchFile);
      return this.processLoadedRecordsInBatchFile(guid, batchFile, fileUpload, sdcSchoolCollectionID);
    } catch (final FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      log.error("File could not be processed exception :: {}", fileUnProcessableException);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError("sdcFileUpload", sdcSchoolCollectionID, fileUnProcessableException.getFileError() + " :: " + fileUnProcessableException.getReason());
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError("sdcFileUpload", sdcSchoolCollectionID, e.getMessage());
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
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
   * System was able to process the file successfully, now the data is persisted and saga data is created for further processing.
   * Process loaded records in batch file set.
   * this method will convert from batch file to header and student record,
   * send them to service for persistence and then return the set for further processing.
   *
   * @param guid             the guid
   * @param batchFile        the batch file
   */
  private SdcSchoolCollectionEntity processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final SdcBatchFile batchFile, @NonNull final SdcFileUpload fileUpload, @NonNull final String sdcSchoolCollectionID) {
    log.info("Going to persist data for batch :: {}", guid);
    final SdcSchoolCollectionEntity entity = mapper.toSdcBatchEntityLoaded(batchFile, fileUpload, sdcSchoolCollectionID); // batch file can be processed further and persisted.
    for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      final var sdcBatchStudentEntity = mapper.toSdcSchoolStudentEntity(student, entity);
      entity.getSDCSchoolStudentEntities().add(sdcBatchStudentEntity);
    }
    return markInitialLoadComplete(entity, sdcSchoolCollectionID);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  @Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public SdcSchoolCollectionEntity markInitialLoadComplete(@NonNull final SdcSchoolCollectionEntity sdcSchoolCollectionEntity, @NonNull final String sdcSchoolCollectionID) {
    var schoolCollection = sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolCollectionID));
    if(schoolCollection.isPresent()) {
      var coll = schoolCollection.get();
      coll.getSDCSchoolStudentEntities().clear();
      coll.getSDCSchoolStudentEntities().addAll(sdcSchoolCollectionEntity.getSDCSchoolStudentEntities());
      coll.setUploadDate(sdcSchoolCollectionEntity.getUploadDate());
      coll.setUploadFileName(sdcSchoolCollectionEntity.getUploadFileName());
      coll.setUpdateUser(sdcSchoolCollectionEntity.getUpdateUser());
      coll.setUpdateDate(LocalDateTime.now());
      return sdcSchoolCollectionService.saveSdcSchoolCollection(coll);
    }else{
      throw new StudentDataCollectionAPIRuntimeException("SDC School Collection ID provided :: " + sdcSchoolCollectionID + " :: is not valid");
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
      throw new FileUnProcessableException(INVALID_TRAILER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
    }
    String studentCount = rawTrailer.substring(3,9).trim();
    if(!StringUtils.isNumeric(studentCount)){
      throw new FileUnProcessableException(INVALID_TRAILER_STUDENT_COUNT, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
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
  private SdcStudentDetails getStudentDetailRecordFromFile(final DataSet ds, final String guid, final long index) throws FileUnProcessableException {
    final var transactionCode = ds.getString(TRANSACTION_CODE.getName());
    if (!TRANSACTION_CODE_STUDENT_DETAILS_RECORD.equals(transactionCode)) {
      throw new FileUnProcessableException(INVALID_TRANSACTION_CODE_STUDENT_DETAILS, guid, SdcSchoolCollectionStatus.LOAD_FAIL, String.valueOf(index), ds.getString(LOCAL_STUDENT_ID.getName()));
    }
    return SdcStudentDetails.builder()
      .transactionCode(transactionCode)
      .localStudentID(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LOCAL_STUDENT_ID.getName())))
      .pen(ds.getString(PEN.getName()))
      .legalSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_SURNAME.getName())))
      .legalGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_GIVEN_NAME.getName())))
      .legalMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(LEGAL_MIDDLE_NAME.getName())))
      .usualSurname(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_SURNAME.getName())))
      .usualGivenName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_GIVEN_NAME.getName())))
      .usualMiddleName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(USUAL_MIDDLE_NAME.getName())))
      .birthDate(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(BIRTH_DATE.getName())))
      .gender(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(GENDER.getName())))
      .specialEducationCategory(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(SPECIAL_EDUCATION_CATEGORY.getName())))
      .unusedBlock1(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(UNUSED_BLOCK1.getName())))
      .schoolFundingCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(SCHOOL_FUNDING_CODE.getName())))
      .nativeAncestryIndicator(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(NATIVE_ANCESTRY_INDICATOR.getName())))
      .homeSpokenLanguageCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(HOME_SPOKEN_LANGUAGE_CODE.getName())))
      .unusedBlock2(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(UNUSED_BLOCK2.getName())))
      .otherCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(OTHER_COURSES.getName())))
      .supportBlocks(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(SUPPORT_BLOCKS.getName())))
      .enrolledGradeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(ENROLLED_GRADE_CODE.getName())))
      .enrolledProgramCodes(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(ENROLLED_PROGRAM_CODES.getName())))
      .careerProgramCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(CAREER_PROGRAM_CODE.getName())))
      .numberOfCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(NUMBER_OF_COURSES.getName())))
      .bandCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(BAND_CODE.getName())))
      .postalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(ds.getString(POSTAL_CODE.getName())))
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

