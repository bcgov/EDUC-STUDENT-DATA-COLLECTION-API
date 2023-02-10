package ca.bc.gov.educ.studentdatacollection.api.exception;

public class SagaRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 5241655513745148898L;

	public SagaRuntimeException(String message) {
		super(message);
	}

	public SagaRuntimeException(Throwable exception) {
		super(exception);
	}

}
