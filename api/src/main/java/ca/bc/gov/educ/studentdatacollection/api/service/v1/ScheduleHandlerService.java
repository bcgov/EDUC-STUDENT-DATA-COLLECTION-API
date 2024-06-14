package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.IndySchoolNoActivityEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.IndySchoolNotSubmittedEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.properties.EmailProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolContact;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.STARTED;

@Service
@Slf4j
public class ScheduleHandlerService {

  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
  protected final SagaRepository sagaRepository;
  private final RestUtils restUtils;
  private final IndySchoolNoActivityEmailOrchestrator indySchoolNoActivityEmailOrchestrator;
  private final IndySchoolNotSubmittedEmailOrchestrator indySchoolNotSubmittedEmailOrchestrator;
  private final EmailProperties emailProperties;

  public ScheduleHandlerService(final SagaRepository sagaRepository, RestUtils restUtils, IndySchoolNoActivityEmailOrchestrator indySchoolNoActivityEmailOrchestrator, IndySchoolNotSubmittedEmailOrchestrator indySchoolNotSubmittedEmailOrchestrator, EmailProperties emailProperties) {
    this.sagaRepository = sagaRepository;
    this.restUtils = restUtils;
    this.indySchoolNoActivityEmailOrchestrator = indySchoolNoActivityEmailOrchestrator;
    this.indySchoolNotSubmittedEmailOrchestrator = indySchoolNotSubmittedEmailOrchestrator;
    this.emailProperties = emailProperties;
  }

  @Transactional
  public void createAndStartUnsubmittedEmailSagas(final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities) {
    List<SdcSagaEntity> sagaEntities = new ArrayList<>();
    for(SdcSchoolCollectionEntity sdcSchoolCollection: sdcSchoolCollectionEntities){
      var emailFields = new HashMap<String, String>();
      emailFields.put("submissionDueDate", formatter.format(sdcSchoolCollection.getCollectionEntity().getSubmissionDueDate()));
      emailFields.put("dataCollectionMonth", getMonthValueForInstructionLink(sdcSchoolCollection.getCollectionEntity().getCollectionTypeCode()));

      if((sdcSchoolCollection.getSdcSchoolCollectionStatusCode().equals(SdcSchoolCollectionStatus.NEW.getCode())) &&
        (sagaRepository.findBySdcSchoolCollectionIDAndSagaNameAndStatusEquals(sdcSchoolCollection.getSdcSchoolCollectionID(), SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA.name(), SagaStatusEnum.COMPLETED.toString()).isEmpty())) {
        var emailSagaData = createEmailSagaData(sdcSchoolCollection.getSchoolID(), emailProperties.getSchoolNotificationEmailFrom(), emailProperties.getEmailSubjectIndependentSchoolNoActivity(), "collection.independent.school.no.activity.notification", emailFields);

        String payload;
        try {
          payload = JsonUtil.getJsonStringFromObject(emailSagaData);
        } catch (JsonProcessingException e) {
          throw new StudentDataCollectionAPIRuntimeException("Exception occurred processing emailSagaData: " + e.getMessage());
        }
        final var saga = createSagaEntity(sdcSchoolCollection.getSdcSchoolCollectionID(), payload, SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA);
        sagaEntities.add(saga);
      }else if((!sdcSchoolCollection.getSdcSchoolCollectionStatusCode().equals(SdcSchoolCollectionStatus.NEW.getCode())) &&
              (sagaRepository.findBySdcSchoolCollectionIDAndSagaNameAndStatusEquals(sdcSchoolCollection.getSdcSchoolCollectionID(),SagaEnum.INDY_SCHOOLS_NOT_SUBMITTED_EMAIL_SAGA.name(), SagaStatusEnum.COMPLETED.toString()).isEmpty())) {
        var emailSagaData = createEmailSagaData(sdcSchoolCollection.getSchoolID(), emailProperties.getSchoolNotificationEmailFrom(), emailProperties.getEmailSubjectIndependentSchoolNotSubmitted(), "collection.independent.school.not.submitted.notification", emailFields);
        String payload;
        try {
          payload = JsonUtil.getJsonStringFromObject(emailSagaData);
        } catch (JsonProcessingException e) {
          throw new StudentDataCollectionAPIRuntimeException("Exception occurred processing emailSagaData: " + e.getMessage());
        }
        final var saga = createSagaEntity(sdcSchoolCollection.getSdcSchoolCollectionID(), payload, SagaEnum.INDY_SCHOOLS_NOT_SUBMITTED_EMAIL_SAGA);
        sagaEntities.add(saga);
      }
    }
    if(!sagaEntities.isEmpty()) {
      var savedSagas = this.indySchoolNotSubmittedEmailOrchestrator.createSagas(sagaEntities);
      savedSagas.forEach(sdcSagaEntity -> {
        if(sdcSagaEntity.getSagaName().equals(SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA.name())){
          this.indySchoolNoActivityEmailOrchestrator.startSaga(sdcSagaEntity);
        }else{
          this.indySchoolNotSubmittedEmailOrchestrator.startSaga(sdcSagaEntity);
        }
      });
    }
  }

  private EmailSagaData createEmailSagaData(UUID schoolID, String fromEmail, String subject, String templateName, HashMap<String,String> emailFields){
    return EmailSagaData
            .builder()
            .fromEmail(fromEmail)
            .toEmails(getPrincipalEmailsForSchool(schoolID))
            .subject(subject)
            .templateName(templateName)
            .emailFields(emailFields)
            .build();
  }

  private SdcSagaEntity createSagaEntity(UUID sdcSchoolCollectionID, String payload, SagaEnum sagaEnum){
    return SdcSagaEntity
            .builder()
            .payload(payload)
            .sdcSchoolCollectionID(sdcSchoolCollectionID)
            .sagaName(sagaEnum.name())
            .status(STARTED.toString())
            .sagaState(INITIATED.toString())
            .createDate(LocalDateTime.now())
            .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
            .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
            .updateDate(LocalDateTime.now())
            .build();
  }

  private String getMonthValueForInstructionLink(String collectionTypeMonth){
    if(collectionTypeMonth.equals(CollectionTypeCodes.JULY.getTypeCode())){
      return "summer-learning";
    }else{
      return collectionTypeMonth.toLowerCase();
    }
  }

  private List<String> getPrincipalEmailsForSchool(UUID schoolID){
    var school = restUtils.getSchoolDetails(schoolID);
    var contacts = school.getContacts().stream().filter(schoolContact -> schoolContact.getSchoolContactTypeCode().equals("PRINCIPAL")).toList();
    return contacts.stream().map(SchoolContact::getEmail).toList();
  }
}
