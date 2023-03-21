package ca.bc.gov.educ.studentdatacollection.api.batch.exception;

import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * The type File un processable exception.
 */
@Slf4j
public class FileUnProcessableException extends Exception {

  /**
   * The constant GUID_IS.
   */
  public static final String GUID_IS = " guid is :: ";
  private static final long serialVersionUID = -3024811399248399591L;
  /**
   * The File error.
   */
  @Getter
  private final FileError fileError;
  /**
   * The Pen request batch status code.
   */
  @Getter
  private final SdcBatchStatusCodes studentCollectionBatchStatusCodes;
  /**
   * The Reason.
   */
  @Getter
  private final String reason;

  /**
   * Instantiates a new File un processable exception.
   *
   * @param fileError                 the file error
   * @param guid                      the guid
   * @param studentCollectionBatchStatusCodes the pen request batch status code
   * @param messageArgs               the message args
   */
  public FileUnProcessableException(final FileError fileError, final String guid, final SdcBatchStatusCodes studentCollectionBatchStatusCodes, final String... messageArgs) {
    super(fileError.getMessage() + GUID_IS + guid);
    this.fileError = fileError;
    this.studentCollectionBatchStatusCodes = studentCollectionBatchStatusCodes;
    var finalLogMessage = fileError.getMessage();
    if (messageArgs != null) {
      finalLogMessage = getFormattedMessage(finalLogMessage, messageArgs);
    }
    log.error(finalLogMessage + GUID_IS + guid);
    this.reason = finalLogMessage;
  }

  /**
   * Gets formatted message.
   *
   * @param msg           the msg
   * @param substitutions the substitutions
   * @return the formatted message
   */
  private static String getFormattedMessage(final String msg, final String... substitutions) {
    final String format = msg.replace("$?", "%s");
    return String.format(format, (Object[]) substitutions);
  }
}
