package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.IndySchoolNoActivityEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.IndySchoolNotSubmittedEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.ProvincialDupliatesEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.email.DistrictSignoffNotificationEmailOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.properties.EmailProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SagaRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolContact;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection1701Users;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.INITIATED;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.STARTED;

@Service
@Slf4j
public class ScheduleHandlerService {

  private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
  private static final Logger logger = LoggerFactory.getLogger(ScheduleHandlerService.class);
  protected final SagaRepository sagaRepository;
  private final RestUtils restUtils;
  private final IndySchoolNoActivityEmailOrchestrator indySchoolNoActivityEmailOrchestrator;
  private final IndySchoolNotSubmittedEmailOrchestrator indySchoolNotSubmittedEmailOrchestrator;
  private final ProvincialDupliatesEmailOrchestrator provincialDupliatesEmailOrchestrator;
  private final DistrictSignoffNotificationEmailOrchestrator districtSignoffNotificationEmailOrchestrator;
  private final EmailProperties emailProperties;

  public ScheduleHandlerService(final SagaRepository sagaRepository, RestUtils restUtils, IndySchoolNoActivityEmailOrchestrator indySchoolNoActivityEmailOrchestrator, IndySchoolNotSubmittedEmailOrchestrator indySchoolNotSubmittedEmailOrchestrator, ProvincialDupliatesEmailOrchestrator provincialDupliatesEmailOrchestrator, DistrictSignoffNotificationEmailOrchestrator districtSignoffNotificationEmailOrchestrator, EmailProperties emailProperties) {
    this.sagaRepository = sagaRepository;
    this.restUtils = restUtils;
    this.indySchoolNoActivityEmailOrchestrator = indySchoolNoActivityEmailOrchestrator;
    this.indySchoolNotSubmittedEmailOrchestrator = indySchoolNotSubmittedEmailOrchestrator;
    this.provincialDupliatesEmailOrchestrator = provincialDupliatesEmailOrchestrator;
    this.districtSignoffNotificationEmailOrchestrator = districtSignoffNotificationEmailOrchestrator;
    this.emailProperties = emailProperties;
  }

  @Transactional
  public void createAndStartUnsubmittedEmailSagas(final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities) {
    List<SdcSagaEntity> sagaEntities = new ArrayList<>();
    for(SdcSchoolCollectionEntity sdcSchoolCollection: sdcSchoolCollectionEntities){
      var emailFields = new HashMap<String, String>();
      emailFields.put("submissionDueDate", formatter.format(sdcSchoolCollection.getCollectionEntity().getSubmissionDueDate()));
      emailFields.put("dataCollectionMonth", getMonthValueForInstructionLink(sdcSchoolCollection.getCollectionEntity().getCollectionTypeCode()));
      var principalEmails = getPrincipalEmailsForSchool(sdcSchoolCollection.getSchoolID());

      if(!principalEmails.isEmpty()) {
        if (sdcSchoolCollection.getSdcSchoolCollectionStatusCode().equals(SdcSchoolCollectionStatus.NEW.getCode())) {
          var emailSagaData = createEmailSagaData(emailProperties.getSchoolNotificationEmailFrom(), principalEmails, emailProperties.getEmailSubjectIndependentSchoolNoActivity(), "collection.independent.school.no.activity.notification", emailFields);

          String payload;
          try {
            payload = JsonUtil.getJsonStringFromObject(emailSagaData);
          } catch (JsonProcessingException e) {
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred processing emailSagaData: " + e.getMessage());
          }
          final var saga = createSagaEntity(sdcSchoolCollection.getSdcSchoolCollectionID(), payload, SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA);
          sagaEntities.add(saga);
        } else if (!sdcSchoolCollection.getSdcSchoolCollectionStatusCode().equals(SdcSchoolCollectionStatus.NEW.getCode())) {
          var emailSagaData = createEmailSagaData(emailProperties.getSchoolNotificationEmailFrom(), principalEmails, emailProperties.getEmailSubjectIndependentSchoolNotSubmitted(), "collection.independent.school.not.submitted.notification", emailFields);
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
    }
    if(!sagaEntities.isEmpty()) {
      var savedSagas = this.indySchoolNotSubmittedEmailOrchestrator.createSagas(sagaEntities);
      savedSagas.forEach(sdcSagaEntity -> {
        if(sdcSagaEntity.getSagaName().equals(SagaEnum.INDY_SCHOOLS_NO_ACTIVITY_EMAIL_SAGA.name())){
          log.info("Starting indySchoolNoActivityEmailOrchestrator saga with the following payload :: {}", sdcSagaEntity);
          this.indySchoolNoActivityEmailOrchestrator.startSaga(sdcSagaEntity);
        }else{
          log.info("Starting indySchoolNotSubmittedEmailOrchestrator saga with the following payload :: {}", sdcSagaEntity);
          this.indySchoolNotSubmittedEmailOrchestrator.startSaga(sdcSagaEntity);
        }
      });
    }
  }

  @Transactional
  public List<SdcSagaEntity> createAndStartProvinceDuplicateEmailSagas(Map<UUID, SdcSchoolCollection1701Users> schoolCollectionEmailMap, String dueDate){
    List<SdcSagaEntity> sagaEntities = new ArrayList<>();
    for(Map.Entry<UUID, SdcSchoolCollection1701Users> entry : schoolCollectionEmailMap.entrySet()){
      var emailFields = new HashMap<String, String>();
      emailFields.put("schoolName", entry.getValue().getSchoolDisplayName());
      emailFields.put("duplicateResolutionDueDate", dueDate);

      var emailSagaData = createProvincialDuplicateEmailSagaData(emailProperties.getSchoolNotificationEmailFrom(),
              entry.getValue().getEmails(), emailProperties.getEmailSubjectProvincialDuplicates(),
              "collection.provincial.duplicates.notification", emailFields);

      String payload;
      try {
        payload = JsonUtil.getJsonStringFromObject(emailSagaData);
      } catch (JsonProcessingException e) {
        throw new StudentDataCollectionAPIRuntimeException("Exception occurred processing emailSagaData: " + e.getMessage());
      }

      final var saga = createSagaEntity(entry.getKey(), payload, SagaEnum.PROVINCE_DUPLICATE_PROCESSING_SAGA);
      sagaEntities.add(saga);
    }
    if(!sagaEntities.isEmpty()) {
      return this.provincialDupliatesEmailOrchestrator.createSagas(sagaEntities);
    }
    return new ArrayList<>();
  }

  public void startCreatedEmailSagas(List<SdcSagaEntity> savedSagas){
    log.info("Starting provincialDupliatesEmailOrchestrator saga with the following payloads :: {}", savedSagas);
    savedSagas.forEach(this.provincialDupliatesEmailOrchestrator::startSaga);
  }

  @Transactional
  public List<SdcSagaEntity> createAndStartDistrictSignoffNotificationEmailSagas(Map<UUID, Set<String>> districtCollectionEmailMap, String signOffDueDate) {
    List<SdcSagaEntity> sagaEntities = new ArrayList<>();
    for (Map.Entry<UUID, Set<String>> entry : districtCollectionEmailMap.entrySet()) {
      if (entry.getValue().isEmpty()) {
        continue;
      }
      var emailFields = new HashMap<String, String>();
      emailFields.put("signOffDueDate", signOffDueDate);

      var emailSagaData = createProvincialDuplicateEmailSagaData(
              emailProperties.getSchoolNotificationEmailFrom(),
              entry.getValue(),
              emailProperties.getEmailSubjectDistrictSignoffNotification(),
              "collection.district.signoff.notification",
              emailFields);

      String payload;
      try {
        payload = JsonUtil.getJsonStringFromObject(emailSagaData);
      } catch (JsonProcessingException e) {
        throw new StudentDataCollectionAPIRuntimeException("Exception occurred processing emailSagaData: " + e.getMessage());
      }

      final var saga = createSagaEntity(entry.getKey(), payload, SagaEnum.DISTRICT_SIGNOFF_NOTIFICATION_EMAIL_SAGA);
      sagaEntities.add(saga);
    }
    if (!sagaEntities.isEmpty()) {
      var savedSagas = this.districtSignoffNotificationEmailOrchestrator.createSagas(sagaEntities);
      log.info("Starting districtSignoffNotificationEmailOrchestrator sagas for {} districts", savedSagas.size());
      savedSagas.forEach(this.districtSignoffNotificationEmailOrchestrator::startSaga);
      return savedSagas;
    }
    return new ArrayList<>();
  }

  private EmailSagaData createEmailSagaData(String fromEmail, List<String> principals, String subject, String templateName, HashMap<String,String> emailFields){
    return EmailSagaData
            .builder()
            .fromEmail(fromEmail)
            .toEmails(principals)
            .subject(subject)
            .templateName(templateName)
            .emailFields(emailFields)
            .build();
  }

  private EmailSagaData createProvincialDuplicateEmailSagaData(String fromEmail, Set<String> emailList, String subject, String templateName, HashMap<String, String> emailFields){
    return EmailSagaData
            .builder()
            .fromEmail(fromEmail)
            .toEmails(new ArrayList<>(emailList))
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

    if (contacts.isEmpty()){
      logger.warn("Warning: no Principal found for school with ID {}", schoolID);
      return Collections.emptyList();
    }

    return contacts.stream()
            .filter(schoolContact -> StringUtils.isNotBlank(schoolContact.getEmail()))
            .map(SchoolContact::getEmail).toList();
  }
}
