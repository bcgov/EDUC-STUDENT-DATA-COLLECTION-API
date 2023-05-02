package ca.bc.gov.educ.studentdatacollection.api.batch.validator;

import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileError;
import ca.bc.gov.educ.studentdatacollection.api.batch.exception.FileUnProcessableException;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.extern.slf4j.Slf4j;
import net.sf.flatpack.DataError;
import net.sf.flatpack.DataSet;
import net.sf.flatpack.Record;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * this class is responsible to validate the batch file that was uploaded.
 */
@Component
@Slf4j
public class SdcFileValidator {

  private static final String HEADER_LENGTH_ERROR = "SHOULD BE 59";
  private static final String DETAIL_LENGTH_ERROR = "SHOULD BE 234";
  private static final String TRAILER_LENGTH_ERROR = "SHOULD BE 224";

  public static final String HEADER_STARTS_WITH = "FFI";
  public static final String TRAILER_STARTS_WITH = "BTR";
  public static final String TOO_LONG = "TOO LONG";

  public void validateFileForFormatAndLength(@NonNull final String guid, @NonNull final DataSet ds) throws FileUnProcessableException {
    if (ds.getErrors() != null && !ds.getErrors().isEmpty()) {
      this.validateHeaderWhenFileHasLengthErrors(guid, ds);
      this.validateTrailerWhenFileHasLengthErrors(guid, ds);
    } else {
      this.validateHeaderTrailerWhenFileHasNoLengthErrors(guid, ds);
    }
    this.processDataSetForRowLengthErrors(guid, ds);
  }

  public void validateFileHasCorrectExtension(
    @NonNull final String guid,
    final SdcFileUpload fileUpload
  ) throws FileUnProcessableException {
    String fileName = fileUpload.getFileName();
    String extension = fileName.substring(fileName.lastIndexOf('.') + 1);

    if (!"ver".equals(extension) && !"std".equals(extension)) {
      throw new FileUnProcessableException(
        FileError.INVALID_FILETYPE,
        guid,
        SdcSchoolCollectionStatus.LOAD_FAIL
      );
    }
  }

  public void validateFileHasCorrectMincode(
    @NonNull final String guid,
    @NonNull final DataSet ds,
    final Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntity,
    final RestUtils restUtils
  ) throws FileUnProcessableException {
    if (!sdcSchoolCollectionEntity.isEmpty()) {
      String schoolID = sdcSchoolCollectionEntity.get().getSchoolID().toString();
      Optional<School> school = restUtils.getSchoolBySchoolID(schoolID);

      if (school.isEmpty()) {
        throw new FileUnProcessableException(
          FileError.INVALID_SCHOOL,
          guid,
          SdcSchoolCollectionStatus.LOAD_FAIL,
          schoolID
        );
      }

      ds.goTop();
      ds.next();

      Optional<Record> dsHeader = ds.getRecord();
      if (dsHeader.isEmpty()) {
        throw new FileUnProcessableException(
          FileError.MISSING_HEADER,
          guid,
          SdcSchoolCollectionStatus.LOAD_FAIL
        );
      }

      String fileMincode = dsHeader.get().getString("mincode");
      String mincode = school.get().getMincode();
      if (!fileMincode.equals(mincode)) {
        throw new FileUnProcessableException(
          FileError.MINCODE_MISMATCH,
          guid,
          SdcSchoolCollectionStatus.LOAD_FAIL,
          mincode
        );
      }

      ds.goTop();
    }
  }

  private void validateTrailerWhenFileHasLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    final int totalRecords = ds.getRowCount() + ds.getErrorCount();
    final Optional<DataError> isErrorOnLastLineOptional = ds.getErrors().stream().filter(el -> el.getLineNo() == totalRecords).findFirst();
    if (isErrorOnLastLineOptional.isPresent()) {
      if (!StringUtils.startsWith(isErrorOnLastLineOptional.get().getRawData(), TRAILER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
      }
    } else {
      ds.goBottom();
      if (!StringUtils.startsWith(ds.getRawData(), TRAILER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
      }
      ds.goTop(); // reset and move the cursor to top as everything is fine.
    }
  }

  private void validateHeaderWhenFileHasLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    final Optional<DataError> isErrorOnFirstLineOptional = ds.getErrors().stream().filter(el -> el.getLineNo() == 1).findFirst();
    if (isErrorOnFirstLineOptional.isPresent()) {
      if (!StringUtils.startsWith(isErrorOnFirstLineOptional.get().getRawData(), HEADER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
      }
    } else {
      ds.goTop();
      ds.next();
      if (!StringUtils.startsWith(ds.getRawData(), HEADER_STARTS_WITH)) {
        throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
      }
    }
  }

  private void validateHeaderTrailerWhenFileHasNoLengthErrors(final String guid, final DataSet ds) throws FileUnProcessableException {
    ds.goTop();
    ds.next();
    if (!StringUtils.startsWith(ds.getRawData(), HEADER_STARTS_WITH)) {
      throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_HEADER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
    }
    ds.goBottom();
    if (!StringUtils.startsWith(ds.getRawData(), TRAILER_STARTS_WITH)) {
      throw new FileUnProcessableException(FileError.INVALID_TRANSACTION_CODE_TRAILER, guid, SdcSchoolCollectionStatus.LOAD_FAIL);
    }
    ds.goTop(); // reset and move the cursor to top as everything is fine.
  }

  private static boolean isMalformedRowError(DataError error) {
    String description = error.getErrorDesc();

    if (description.contains(HEADER_LENGTH_ERROR)
    || description.contains(DETAIL_LENGTH_ERROR)
    || description.contains(TRAILER_LENGTH_ERROR)) {
      return true;
    }

    return false;
  }

  private String getMalformedRowMessage(String errorDescription, DataError error) {
    if (errorDescription.contains(HEADER_LENGTH_ERROR)) {
      return this.getHeaderRowLengthIncorrectMessage(errorDescription);
    } else if (errorDescription.contains(DETAIL_LENGTH_ERROR)) {
      return this.getDetailRowLengthIncorrectMessage(error, errorDescription);
    } else if (errorDescription.contains(TRAILER_LENGTH_ERROR)) {
      return this.getTrailerRowLengthIncorrectMessage(errorDescription);
    }
    return "The uploaded file contains a malformed error that could not be identified.";
  }

  /**
   * Process data set for row length errors.
   *
   * @param guid the guid
   * @param ds   the ds
   * @throws FileUnProcessableException the file un processable exception
   */
  private void processDataSetForRowLengthErrors(
    @NonNull final String guid,
    @NonNull final DataSet ds
  ) throws FileUnProcessableException {
    Optional<DataError> maybeError = ds
    .getErrors()
    .stream()
    .filter((e) -> isMalformedRowError(e))
    .findFirst();

    if (maybeError.isPresent()) {
      DataError error = maybeError.get();
      String message = this.getMalformedRowMessage(error.getErrorDesc(), error);
      throw new FileUnProcessableException(
        FileError.INVALID_ROW_LENGTH,
        guid,
        SdcSchoolCollectionStatus.LOAD_FAIL,
        message
      );
    }
  }

  /**
   * Gets header row length incorrect message.
   *
   * @param description the DataError description
   * @return the right error description
   */
  private String getHeaderRowLengthIncorrectMessage(String description) {
    if (description.contains(TOO_LONG)) {
      return "Header record has extraneous characters.";
    }
    return "Header record is missing characters.";
  }

  /**
   * Gets trailer row length incorrect message.
   *
   * @param errorDescription the {@link DataError} description
   * @return the trailer row length incorrect message
   */
  private String getTrailerRowLengthIncorrectMessage(String errorDescription) {
    if (errorDescription.contains(TOO_LONG)) {
      return "Trailer record has extraneous characters.";
    }
    return "Trailer record is missing characters.";
  }

  /**
   * Gets detail row length incorrect message.
   * here 1 is subtracted from the line number as line number starts from header record and here header record
   * needs to
   * be  discarded
   *
   * @param errorDescription the {@link DataError} description
   * @param error the error
   * @return the detail row length incorrect message
   */
  private String getDetailRowLengthIncorrectMessage(
    final DataError error,
    String errorDescription
  ) {
    if (errorDescription.contains(TOO_LONG)) {
      return "Detail record " + (error.getLineNo() - 1) + " has extraneous characters.";
    }
    return "Detail record " + (error.getLineNo() - 1) + " is missing characters.";
  }

  /**
   * Process student count for mismatch and size.
   *
   * @param guid      the guid
   * @param batchFile the batch file
   * @throws FileUnProcessableException the file un processable exception
   */
  public void validateStudentCountForMismatchAndSize(final String guid, final SdcBatchFile batchFile) throws FileUnProcessableException {
    final var studentCount = batchFile.getBatchFileTrailer().getStudentCount();
    if (!StringUtils.isNumeric(studentCount) || Integer.parseInt(studentCount) != batchFile.getStudentDetails().size()) {
      throw new FileUnProcessableException(FileError.STUDENT_COUNT_MISMATCH, guid, SdcSchoolCollectionStatus.LOAD_FAIL, studentCount, String.valueOf(batchFile.getStudentDetails().size()));
    }
  }
}
