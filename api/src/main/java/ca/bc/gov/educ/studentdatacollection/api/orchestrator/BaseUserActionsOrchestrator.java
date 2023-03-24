package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import lombok.extern.slf4j.Slf4j;

/**
 * The type Base user actions orchestrator.
 *
 * @param <T> the type parameter
 */
@Slf4j
public abstract class BaseUserActionsOrchestrator<T> extends BaseOrchestrator<T> {

  /**
   * Instantiates a new Base user actions orchestrator.
   *
   * @param sagaService      the saga service
   * @param messagePublisher the message publisher
   * @param clazz            the clazz
   * @param sagaName         the saga name
   * @param topicToSubscribe the topic to subscribe
   */
  protected BaseUserActionsOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final Class<T> clazz, final String sagaName, final String topicToSubscribe) {
    super(sagaService, messagePublisher, clazz, sagaName, topicToSubscribe);
  }

}
