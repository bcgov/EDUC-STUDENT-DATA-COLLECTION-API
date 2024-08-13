package ca.bc.gov.educ.studentdatacollection.api.properties;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class EmailProperties {

  @Value("${school.notification.email.from}")
  private String schoolNotificationEmailFrom;

  @Value("${email.subject.collection.independent.school.no.activity.notification}")
  private String emailSubjectIndependentSchoolNoActivity;

  @Value("${email.subject.collection.independent.school.not.submitted.notification}")
  private String emailSubjectIndependentSchoolNotSubmitted;

  @Value("${email.subject.collection.provincial.duplicates.notification}")
  private String emailSubjectProvincialDuplicates;

  @Value("${email.subject.closure.notification}")
  private String emailSubjectClosureNotification;

  @Value("${email.closure.notification.to}")
  private String closureNotificationTo;

}


