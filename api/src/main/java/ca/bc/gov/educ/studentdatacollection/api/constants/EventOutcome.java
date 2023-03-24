package ca.bc.gov.educ.studentdatacollection.api.constants;

/**
 * The enum Event outcome.
 */
public enum EventOutcome {
  VALIDATION_SUCCESS_NO_ERROR_WARNING,
  VALIDATION_SUCCESS_WITH_ERROR,
  PEN_MATCH_PROCESSED,
  PEN_MATCH_RESULTS_PROCESSED,
  READ_FROM_TOPIC_SUCCESS,
  INITIATE_SUCCESS,
  SAGA_COMPLETED
}
