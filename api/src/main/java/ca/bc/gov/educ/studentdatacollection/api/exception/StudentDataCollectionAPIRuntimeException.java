package ca.bc.gov.educ.studentdatacollection.api.exception;

/**
 * The type Pen reg api runtime exception.
 */
public class StudentDataCollectionAPIRuntimeException extends RuntimeException {

  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 5241655513745148898L;

  /**
   * Instantiates a new Pen reg api runtime exception.
   *
   * @param message the message
   */
  public StudentDataCollectionAPIRuntimeException(String message) {
		super(message);
	}

  public StudentDataCollectionAPIRuntimeException(Throwable exception) {
    super(exception);
  }

}
