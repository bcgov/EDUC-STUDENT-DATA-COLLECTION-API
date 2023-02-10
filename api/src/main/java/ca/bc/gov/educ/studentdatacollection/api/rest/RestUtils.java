package ca.bc.gov.educ.studentdatacollection.api.rest;

import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValidationIssueFieldTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValidationIssueTypeCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
@Component
@Slf4j
public class RestUtils {
  public static final String GRADE_CODES = "gradeCodes";
  private static final String CONTENT_TYPE = "Content-Type";
  private final ObjectMapper obMapper = new ObjectMapper();
  /**
   * The Props.
   */

  @Getter
  private final ApplicationProperties props;


  private final WebClient webClient;

  private final Map<String, ValidationIssueTypeCode> validationIssueTypeCodeMap = new ConcurrentHashMap<>();

  private final Map<String, ValidationIssueFieldTypeCode> validationIssueFieldTypeCodeMap = new ConcurrentHashMap<>();

  /**
   * The School lock.
   */
  private final ReadWriteLock schoolLock = new ReentrantReadWriteLock();
  @Value("${initialization.background.enabled}")
  private Boolean isBackgroundInitializationEnabled;

  /**
   * Instantiates a new Rest utils.
   *
   * @param props the props
   */
  @Autowired
  public RestUtils(final ApplicationProperties props, final WebClient webClient) {
    this.props = props;
    this.webClient = webClient;
  }

  /**
   * Init.
   */
  @PostConstruct
  public void init() {
    if (this.isBackgroundInitializationEnabled != null && this.isBackgroundInitializationEnabled) {
      ApplicationProperties.bgTask.execute(this::initialize);
    }
  }

  private void initialize() {
  }

  /**
   * Scheduled.
   */
  @Scheduled(cron = "${schedule.jobs.load.school.cron}")
  public void scheduled() {
    val writeLock = this.schoolLock.writeLock();
    try {
      writeLock.lock();
      this.init();
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Gets penRequestBatchStudentValidationIssueTypeCode by issue type code.
   *
   * @param issueTypeCode the issue type code
   * @return the PenRequestBatchStudentValidationIssueTypeCode
   */
  public Optional<ValidationIssueTypeCode> getValidationIssueTypeCodeInfoByIssueTypeCode(final String issueTypeCode) {
    return Optional.ofNullable(this.validationIssueTypeCodeMap.get(issueTypeCode));
  }

  /**
   * Gets penRequestBatchStudentValidationIssueTypeCode by issue field code.
   *
   * @param issueFieldCode the issue field code
   * @return the PenRequestBatchStudentValidationIssueFieldCode
   */
  public Optional<ValidationIssueFieldTypeCode> getValidationFieldCodeInfoByIssueFieldCode(final String issueFieldCode) {
    return Optional.ofNullable(this.validationIssueFieldTypeCodeMap.get(issueFieldCode));
  }

}
