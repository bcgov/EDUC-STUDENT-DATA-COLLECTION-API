package ca.bc.gov.educ.studentdatacollection.api.batch.processor;


import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileError;
import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.studentdatacollection.api.batch.mappers.SdcBatchFileMapper;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileHeader;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFileTrailer;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcStudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.batch.validator.SdcFileValidator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.mappers.StringMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import com.google.common.base.Stopwatch;
import com.nimbusds.jose.util.Pair;
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
import java.util.concurrent.atomic.AtomicInteger;

import static ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileError.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchFileConstants.*;
import static lombok.AccessLevel.PRIVATE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
@Slf4j
public class SdcBatchFileProcessor {

  /**
   * The constant mapper.
   */
  private static final SdcBatchFileMapper mapper = SdcBatchFileMapper.mapper;
  @Getter(PRIVATE)
  public static final String TRANSACTION_CODE_STUDENT_DETAILS_RECORD = "SRM";
  public static final String SDC_FILE_UPLOAD = "sdcFileUpload";
  public static final String INVALID_PAYLOAD_MSG = "Payload contains invalid data.";

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
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Getter(PRIVATE)
  private final CollectionRepository sdcRepository;

  @Autowired
  public SdcBatchFileProcessor(final ApplicationProperties applicationProperties, final RestUtils restUtils, SdcFileValidator sdcFileValidator, SdcSchoolCollectionService sdcSchoolCollectionService, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, CollectionRepository sdcRepository) {
    this.applicationProperties = applicationProperties;
    this.sdcFileValidator = sdcFileValidator;
    this.restUtils = restUtils;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.sdcRepository = sdcRepository;
  }

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

      this.sdcFileValidator.validateFileHasCorrectExtension(guid, fileUpload);
      this.sdcFileValidator.validateFileForFormatAndLength(guid, ds);

      var schoolGet = getSchoolFromFileMincodeField(guid, ds);
      var sdcSchoolCollection = this.retrieveSdcSchoolCollectionByID(sdcSchoolCollectionID, schoolGet.getMincode(), guid);
      this.resetFileUploadMetadata(sdcSchoolCollection);

      this.sdcFileValidator.validateFileHasCorrectMincode(guid, ds, sdcSchoolCollection);
      this.sdcFileValidator.validateFileUploadIsNotInProgress(guid, ds, sdcSchoolCollection);
      this.populateBatchFile(guid, ds, batchFile);
      this.sdcFileValidator.validateStudentCountForMismatchAndSize(guid, batchFile);

      return this.processLoadedRecordsInBatchFile(guid, batchFile, fileUpload, sdcSchoolCollectionID, false);
    } catch (final FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      log.error("File could not be processed exception :: {}", fileUnProcessableException);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError(SDC_FILE_UPLOAD, sdcSchoolCollectionID, fileUnProcessableException.getFileError() + " :: " + fileUnProcessableException.getReason());
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError(SDC_FILE_UPLOAD, sdcSchoolCollectionID, FileError.GENERIC_ERROR_MESSAGE.getMessage());
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

  @Transactional(propagation = Propagation.MANDATORY)
  public SdcSchoolCollectionEntity processDistrictSdcBatchFile(@NonNull final SdcFileUpload fileUpload, String sdcDistrictCollectionID) {

    val stopwatch = Stopwatch.createStarted();
    final var guid = UUID.randomUUID().toString(); // this guid will be used throughout the logs for easy tracking.
    log.info("Started processing SDC file with district collection ID :: {} and correlation guid :: {}", sdcDistrictCollectionID, guid);
    val batchFile = new SdcBatchFile();
    Optional<Reader> batchFileReaderOptional = Optional.empty();
    try (final Reader mapperReader = new FileReader(Objects.requireNonNull(this.getClass().getClassLoader().getResource("mapper.xml")).getFile())) {

      var byteArrayOutputStream = new ByteArrayInputStream(Base64.getDecoder().decode(fileUpload.getFileContents()));
      batchFileReaderOptional = Optional.of(new InputStreamReader(byteArrayOutputStream));
      final DataSet ds = DefaultParserFactory.getInstance().newFixedLengthParser(mapperReader, batchFileReaderOptional.get()).setStoreRawDataToDataError(true).setStoreRawDataToDataSet(true).setNullEmptyStrings(true).parse();

      this.sdcFileValidator.validateFileForFormatAndLength(guid, ds);

      var schoolGet = getSchoolFromFileMincodeField(guid, ds);

      var districtCollection = this.sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(UUID.fromString(sdcDistrictCollectionID));
      var districtCollectionGet = districtCollection.get();
      this.sdcFileValidator.validateSchoolIsOpenAndBelongsToDistrict(guid, schoolGet, String.valueOf(districtCollectionGet.getDistrictID()));

      var sdcSchoolCollection = this.retrieveSdcSchoolCollectionBySchoolID(schoolGet.getSchoolId(), schoolGet.getMincode(), guid);
      var sdcSchoolCollectionID = sdcSchoolCollection.getSdcSchoolCollectionID();

      this.resetFileUploadMetadata(sdcSchoolCollection);
      this.sdcFileValidator.validateFileHasCorrectExtension(String.valueOf(sdcSchoolCollectionID), fileUpload);

      this.populateBatchFile(guid, ds, batchFile);
      this.sdcFileValidator.validateStudentCountForMismatchAndSize(guid, batchFile);
      this.sdcFileValidator.validateFileUploadIsNotInProgress(guid, ds, sdcSchoolCollection);
      districtCollectionGet.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.NEW.getCode());
      this.sdcDistrictCollectionRepository.save(districtCollectionGet);

      return this.processLoadedRecordsInBatchFile(guid, batchFile, fileUpload, String.valueOf(sdcSchoolCollectionID), true);
    } catch (final FileUnProcessableException fileUnProcessableException) { // system needs to persist the data in this case.
      log.error("File could not be processed exception :: {}", fileUnProcessableException);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError(SDC_FILE_UPLOAD, sdcDistrictCollectionID, fileUnProcessableException.getFileError() + " :: " + fileUnProcessableException.getReason());
      List<FieldError> fieldErrorList = new ArrayList<>();
      fieldErrorList.add(validationError);
      error.addValidationErrors(fieldErrorList);
      throw new InvalidPayloadException(error);
    } catch (final Exception e) { // need to check what to do in case of general exception.
      log.error("Exception while processing the file with guid :: {} :: Exception :: {}", guid, e);
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_PAYLOAD_MSG).status(BAD_REQUEST).build();
      var validationError = ValidationUtil.createFieldError(SDC_FILE_UPLOAD, sdcDistrictCollectionID, FileError.GENERIC_ERROR_MESSAGE.getMessage());
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

  private SchoolTombstone getSchoolFromFileMincodeField(final String guid, final DataSet ds) throws FileUnProcessableException {
    var mincode = this.sdcFileValidator.getSchoolMincode(guid, ds);
    var school = this.sdcFileValidator.getSchoolUsingMincode(mincode);
    return school.orElseThrow(() -> new FileUnProcessableException(FileError.INVALID_SCHOOL, guid, SdcSchoolCollectionStatus.LOAD_FAIL, mincode));
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
  public SdcSchoolCollectionEntity processLoadedRecordsInBatchFile(@NonNull final String guid, @NonNull final SdcBatchFile batchFile, @NonNull final SdcFileUpload fileUpload, @NonNull final String sdcSchoolCollectionID, final boolean isDistrictUpload) {
    log.debug("Going to persist data for batch :: {}", guid);
    final SdcSchoolCollectionEntity entity = mapper.toSdcBatchEntityLoaded(batchFile, fileUpload, sdcSchoolCollectionID); // batch file can be processed further and persisted.
    for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
      final var sdcBatchStudentEntity = mapper.toSdcSchoolStudentEntity(student, entity);
      entity.getSDCSchoolStudentEntities().add(sdcBatchStudentEntity);
    }

    return craftStudentSetAndMarkInitialLoadComplete(entity, sdcSchoolCollectionID, isDistrictUpload);
  }

  @Retryable(maxAttempts = 10, backoff = @Backoff(multiplier = 2, delay = 2000))
  public SdcSchoolCollectionEntity craftStudentSetAndMarkInitialLoadComplete(@NonNull final SdcSchoolCollectionEntity sdcSchoolCollectionEntity, @NonNull final String sdcSchoolCollectionID, final boolean isDistrictUpload) {
    var schoolCollection = sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolCollectionID));
    if(schoolCollection.isPresent()) {
      var coll = schoolCollection.get();
      var pairStudentList = compareAndShoreUpStudentList(schoolCollection.get(), sdcSchoolCollectionEntity);
      coll.setUploadDate(sdcSchoolCollectionEntity.getUploadDate());
      coll.setUploadFileName(sdcSchoolCollectionEntity.getUploadFileName());
      coll.setUploadReportDate(sdcSchoolCollectionEntity.getUploadReportDate());
      coll.setUpdateUser(sdcSchoolCollectionEntity.getUpdateUser());
      coll.setUpdateDate(LocalDateTime.now());
      if(isDistrictUpload) {
        coll.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.DISTRICT_UPLOAD.getCode());
      } else {
        coll.setSdcSchoolCollectionStatusCode(String.valueOf(SdcSchoolCollectionStatus.NEW));
      }

      return sdcSchoolCollectionService.saveSdcSchoolCollection(coll, pairStudentList.getLeft(), pairStudentList.getRight());
    }else{
      throw new StudentDataCollectionAPIRuntimeException("SDC School Collection ID provided :: " + sdcSchoolCollectionID + " :: is not valid");
    }
  }

  private Pair<List<SdcSchoolCollectionStudentEntity>, List<UUID>> compareAndShoreUpStudentList(SdcSchoolCollectionEntity currentCollection, SdcSchoolCollectionEntity incomingCollection){
    Map<Integer, SdcSchoolCollectionStudentEntity> incomingStudentsHashCodes = new HashMap<>();
    Map<Integer, SdcSchoolCollectionStudentEntity> finalStudentsMap = new HashMap<>();
    List<UUID> removedStudents = new ArrayList<>();
    incomingCollection.getSDCSchoolStudentEntities().forEach(student -> incomingStudentsHashCodes.put(student.getUniqueObjectHash(), student));
    log.debug("Found {} current students for collection", currentCollection.getSDCSchoolStudentEntities().size());
    log.debug("Found {} incoming students for collection", incomingStudentsHashCodes.size());

    currentCollection.getSDCSchoolStudentEntities().forEach(currentStudent -> {
      var currentStudentHash = currentStudent.getUniqueObjectHash();
      if(incomingStudentsHashCodes.containsKey(currentStudentHash)  && !currentStudent.getSdcSchoolCollectionStudentStatusCode().equals(SdcSchoolStudentStatus.DELETED.toString())){
        finalStudentsMap.put(currentStudentHash, currentStudent);
      }else{
        removedStudents.add(currentStudent.getSdcSchoolCollectionStudentID());
      }
    });

    AtomicInteger newStudCount = new AtomicInteger();
    incomingStudentsHashCodes.keySet().forEach(incomingStudentHash -> {
      if(!finalStudentsMap.containsKey(incomingStudentHash)){
        newStudCount.getAndIncrement();
        finalStudentsMap.put(incomingStudentHash, incomingStudentsHashCodes.get(incomingStudentHash));
      }
    });

    finalStudentsMap.values().forEach(finalStudent -> finalStudent.setSdcSchoolCollection(currentCollection));
    log.debug("Found {} new students for collection {}", newStudCount, currentCollection.getSdcSchoolCollectionID());
    return Pair.of(finalStudentsMap.values().stream().toList(), removedStudents);
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
      .localStudentID(StringMapper.trimAndUppercase(ds.getString(LOCAL_STUDENT_ID.getName())))
      .pen(ds.getString(PEN.getName()))
      .legalSurname(StringMapper.trimAndUppercase(ds.getString(LEGAL_SURNAME.getName())))
      .legalGivenName(StringMapper.processGivenName(ds.getString(LEGAL_GIVEN_NAME.getName())))
      .legalMiddleName(StringMapper.trimAndUppercase(ds.getString(LEGAL_MIDDLE_NAME.getName())))
      .usualSurname(StringMapper.trimAndUppercase(ds.getString(USUAL_SURNAME.getName())))
      .usualGivenName(StringMapper.processGivenName(ds.getString(USUAL_GIVEN_NAME.getName())))
      .usualMiddleName(StringMapper.trimAndUppercase(ds.getString(USUAL_MIDDLE_NAME.getName())))
      .birthDate(StringMapper.trimAndUppercase(ds.getString(BIRTH_DATE.getName())))
      .gender(StringMapper.trimAndUppercase(ds.getString(GENDER.getName())))
      .specialEducationCategory(StringMapper.trimAndUppercase(ds.getString(SPECIAL_EDUCATION_CATEGORY.getName())))
      .unusedBlock1(StringMapper.trimAndUppercase(ds.getString(UNUSED_BLOCK1.getName())))
      .schoolFundingCode(StringMapper.trimAndUppercase(ds.getString(SCHOOL_FUNDING_CODE.getName())))
      .nativeAncestryIndicator(StringMapper.trimAndUppercase(ds.getString(NATIVE_ANCESTRY_INDICATOR.getName())))
      .homeSpokenLanguageCode(StringMapper.trimAndUppercase(ds.getString(HOME_SPOKEN_LANGUAGE_CODE.getName())))
      .unusedBlock2(StringMapper.trimAndUppercase(ds.getString(UNUSED_BLOCK2.getName())))
      .otherCourses(StringMapper.trimAndUppercase(ds.getString(OTHER_COURSES.getName())))
      .supportBlocks(StringMapper.trimAndUppercase(ds.getString(SUPPORT_BLOCKS.getName())))
      .enrolledGradeCode(StringMapper.trimAndUppercase(ds.getString(ENROLLED_GRADE_CODE.getName())))
      .enrolledProgramCodes(StringMapper.trimAndUppercase(TransformUtil.sanitizeEnrolledProgramString(ds.getString(ENROLLED_PROGRAM_CODES.getName()))))
      .careerProgramCode(StringMapper.trimAndUppercase(ds.getString(CAREER_PROGRAM_CODE.getName())))
      .numberOfCourses(StringMapper.trimAndUppercase(ds.getString(NUMBER_OF_COURSES.getName())))
      .bandCode(StringMapper.trimAndUppercase(ds.getString(BAND_CODE.getName())))
      .postalCode(StringMapper.trimAndUppercase(ds.getString(POSTAL_CODE.getName())))
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
        .reportDate((ds.getString(REPORT_DATE.getName())))
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

  public void resetFileUploadMetadata(SdcSchoolCollectionEntity sdcSchoolCollection){
    sdcSchoolCollection.setUploadFileName(null);
    sdcSchoolCollection.setUploadDate(null);
    sdcSchoolCollection.setUploadReportDate(null);
  }

  public SdcSchoolCollectionEntity retrieveSdcSchoolCollectionByID(String sdcSchoolCollectionID, final String mincode, final String guid) throws FileUnProcessableException {
    var sdcSchoolCollection = sdcSchoolCollectionRepository.findById(UUID.fromString(sdcSchoolCollectionID));
    return getSdcSchoolCollectionOrThrow(sdcSchoolCollection, mincode, guid);
  }

  public SdcSchoolCollectionEntity retrieveSdcSchoolCollectionBySchoolID(String schoolID, final String mincode, final String guid) throws FileUnProcessableException {
    var sdcSchoolCollection = sdcSchoolCollectionRepository.findActiveCollectionBySchoolId(UUID.fromString(schoolID));
    return getSdcSchoolCollectionOrThrow(sdcSchoolCollection, mincode, guid);
  }

  private SdcSchoolCollectionEntity getSdcSchoolCollectionOrThrow(final Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity, final String mincode, final String guid) throws FileUnProcessableException {
    if(sdcSchoolCollectionEntity.isEmpty()){
      throw new FileUnProcessableException(FileError.INVALID_SDC_SCHOOL_COLLECTION_ID, guid, SdcSchoolCollectionStatus.LOAD_FAIL, mincode);
    }

    return sdcSchoolCollectionEntity.get();
  }
}

