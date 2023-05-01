package ca.bc.gov.educ.studentdatacollection.api.batch.processor;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.STUDENT_DATA_COLLECTION_API_TOPIC;


@Component
@Slf4j
public class SdcBatchFileStudentRecordsProcessor {
  /**
   * The Message publisher.
   */
  private final MessagePublisher messagePublisher;


  @Autowired
  public SdcBatchFileStudentRecordsProcessor(final MessagePublisher messagePublisher) {
    this.messagePublisher = messagePublisher;
  }

  /**
   * Publish unprocessed student records for processing.
   * this will publish messages to the topic which this api is listening to so that load is balanced as across pods
   * as api is in queue group durable subscription.
   *
   * @param batchStudentSagaDataSet the student entities
   */
  public void publishUnprocessedStudentRecordsForProcessing(final Set<SdcStudentSagaData> batchStudentSagaDataSet) {
    batchStudentSagaDataSet.forEach(this::sendIndividualStudentAsMessageToTopic);
  }


  /**
   * Send individual student as message to topic consumer.
   */
  private void sendIndividualStudentAsMessageToTopic(final SdcStudentSagaData batchStudentSagaDataSet) {
    final var eventPayload = JsonUtil.getJsonString(batchStudentSagaDataSet);
    if (eventPayload.isPresent()) {
      final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).build();
      final var eventString = JsonUtil.getJsonString(event);
      if (eventString.isPresent()) {
        this.messagePublisher.dispatchMessage(STUDENT_DATA_COLLECTION_API_TOPIC.toString(), eventString.get().getBytes());
      } else {
        log.error("Event Sting is empty, skipping the publish to topic :: {}", batchStudentSagaDataSet);
      }
    } else {
      log.error("Event payload is empty, skipping the publish to topic :: {}", batchStudentSagaDataSet);
    }
  }

}
