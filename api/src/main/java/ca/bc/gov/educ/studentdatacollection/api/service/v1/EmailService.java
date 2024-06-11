package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.util.Map;

@Service
@Slf4j
public class EmailService {

  protected final SagaService sagaService;
  private final SpringTemplateEngine templateEngine;
  private final Map<String, String> templateConfig;
  private final RestUtils restUtils;
  public EmailService(final SagaService sagaService, final SpringTemplateEngine templateEngine, final Map<String, String> templateConfig, RestUtils restUtils) {
    this.sagaService = sagaService;
    this.templateEngine = templateEngine;
    this.templateConfig = templateConfig;
    this.restUtils = restUtils;
  }

  public void sendEmail(final EmailSagaData emailSagaData) {
    log.debug("Sending email");

    final var ctx = new Context();
    emailSagaData.getEmailFields().forEach(ctx::setVariable);

    if (!this.templateConfig.containsKey(emailSagaData.getTemplateName())) {
      throw new StudentDataCollectionAPIRuntimeException("Email template not found for template name :: " + emailSagaData.getTemplateName());
    }

    final var body = this.templateEngine.process(this.templateConfig.get(emailSagaData.getTemplateName()), ctx);

    this.restUtils.sendEmail(emailSagaData.getFromEmail(), emailSagaData.getToEmails(), body, emailSagaData.getSubject());
    log.debug("Email sent successfully");
  }

}
