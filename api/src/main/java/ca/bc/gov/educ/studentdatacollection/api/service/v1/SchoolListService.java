package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SchoolListService {

    public static final String PAGE_SIZE = "pageSize";
    public static final String PAGE_SIZE_VALUE = "100";
    public static final String PAGE_NUMBER = "pageNumber";
    public static final Integer PAGE_COUNT_VALUE = 60;

    private final MessagePublisher messagePublisher;
    private final ObjectMapper objectMapper;

    public SchoolListService(MessagePublisher messagePublisher, ObjectMapper objectMapper) {
        this.messagePublisher = messagePublisher;
        this.objectMapper = objectMapper;
    }

    /**
     * Calls the Institute API to retrieve a list of schools for the given page.
     * Retries on any Exception (except those that are not retryable).
     *
     * @param correlationID a unique identifier for the request
     * @param pageNumber    the page number to fetch
     * @return List of School objects
     */
    @Retryable(retryFor = {Exception.class}, maxAttempts = 6, backoff = @Backoff(multiplier = 2, delay = 2000))
    public List<School> getAllSchoolList(UUID correlationID, String pageNumber) {
        try {
            log.info("Calling Institute API to load all schools to memory, current page {} of 60", (Integer.parseInt(pageNumber) + 1));
            final TypeReference<List<School>> ref = new TypeReference<>() {};
            val event = Event.builder()
                    .sagaId(correlationID)
                    .eventType(EventType.GET_PAGINATED_SCHOOLS)
                    .eventPayload(PAGE_SIZE.concat("=").concat(PAGE_SIZE_VALUE)
                            .concat("&").concat(PAGE_NUMBER).concat("=").concat(pageNumber))
                    .build();
            val responseMessage = messagePublisher
                    .requestMessage(TopicsEnum.INSTITUTE_API_TOPIC.toString(), JsonUtil.getJsonBytesFromObject(event))
                    .completeOnTimeout(null, 5, TimeUnit.SECONDS)
                    .get();
            if (responseMessage != null) {
                log.info("Response from Institute API is good");
                return objectMapper.readValue(responseMessage.getData(), ref);
            } else {
                log.info("Response from Institute API returned empty list");
                throw new Exception("NATS_TIMEOUT " + correlationID);
            }
        } catch (final Exception ex) {
            log.error("Error getting all schools list: ", ex);
            throw new StudentDataCollectionAPIRuntimeException("NATS_TIMEOUT " + correlationID + ex);
        }
    }
}
