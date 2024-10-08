package ca.bc.gov.educ.studentdatacollection.api.batch.exception;

import lombok.Getter;

/**
 * The enum File error.
 *
 * @author OM
 */
public enum FileError {
  /**
   * Upload file did not contain any content.
   */
  EMPTY_FILE("The uploaded file is empty."),
  /**
   * The Invalid mincode header.
   */
  MISSING_HEADER("The file header appears to be missing"),

  /**
   * The Invalid transaction code header.
   */
  INVALID_TRANSACTION_CODE_HEADER("Invalid transaction code on Header record. It must be FFI"),

  /**
   * The Invalid transaction code trailer.
   */
  INVALID_TRANSACTION_CODE_TRAILER("Invalid transaction code on Trailer record. It must be BTR"),

  /**
   * The Invalid mincode header.
   */
  INVALID_MINCODE_HEADER("Invalid Mincode in Header record."),

  /**
   * The Student count mismatch.
   */
  STUDENT_COUNT_MISMATCH("Invalid count in trailer record. Stated was $?, Actual was $?"),

  /**
   * Uploaded file reported 0 students.
   */
  STUDENT_COUNT_NO_STUDENTS("The uploaded file did not contain any student records."),

  /**
   * Invalid trailer
   */
  INVALID_TRAILER("Invalid trailer record. Student count could not be retrieved"),

  /**
   * Invalid trailer for student count
   */
  INVALID_TRAILER_STUDENT_COUNT("Invalid trailer record. Student count was not a numeric value"),

  /**
   * The Invalid transaction code student details.
   */
  INVALID_TRANSACTION_CODE_STUDENT_DETAILS("Invalid transaction code on Detail record $? for student with Local ID $?"),

  /**
   * The filetype ended in the wrong extension and may be the wrong filetype.
   */
  INVALID_FILE_EXTENSION("File extension invalid. Files must be of type \".ver\" or \".std\"."),

  NO_FILE_EXTENSION("No file extension provided. Files must be of type \".ver\" or \".std\"."),

  CONFLICT_FILE_ALREADY_IN_FLIGHT("File is already being processed for this school. Mincode is: $?"),

  INVALID_SDC_SCHOOL_COLLECTION_ID("Unable to find a school collection for mincode $?"),
  /**
   * No record for the provided school ID was found.
   */
  INVALID_SCHOOL("Unable to find a school record for mincode $?"),
  /**
   * School is closed.
   */
  SCHOOL_IS_CLOSED("Invalid school provided - school is closed."),
  /**
   * School is opening.
   */
  SCHOOL_IS_OPENING("Invalid school provided - school is not yet open."),
  /**
   * No record for the provided school ID was found.
   */
  INVALID_SCHOOL_DATES("Invalid school dates - this was not expected."),
  /**
   * No mincode found in file
   */
  MISSING_MINCODE("No mincode found in file."),
  /**
   * The mincode on the uploaded document does not match the collection record.
   */
  MINCODE_MISMATCH("The uploaded file is for another school. Please upload a file for $?"),

  SCHOOL_OUTSIDE_OF_DISTRICT("The school referenced in the uploaded file does not belong to district."),

  /**
   * Invalid row length file error.
   * This will be thrown when any row in the given file is longer or shorter than expected.
   */
  INVALID_ROW_LENGTH("$?"),

  /**
   * The mincode school is currently closed
   */
  INVALID_MINCODE_SCHOOL_CLOSED("Invalid Mincode in Header record - school is closed."),

  /**
   * The Duplicate batch file psi.
   */
  DUPLICATE_BATCH_FILE_PSI("Duplicate file from PSI."),

  /**
   * The Held back for size.
   */
  HELD_BACK_FOR_SIZE("Held Back For Size."),

  /**
   * The held back for sfas code
   */
  HELD_BACK_FOR_SFAS("Held back for SFAS."),

  GENERIC_ERROR_MESSAGE("Unexpected failure during file processing.");

  /**
   * The Message.
   */
  @Getter
  private final String message;

  /**
   * Instantiates a new File error.
   *
   * @param message the message
   */
  FileError(final String message) {
    this.message = message;
  }
}
