package ca.bc.gov.educ.studentdatacollection.api.service.v1.events.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.LogHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.Orchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.properties.EmailProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionsForAutoSubmit;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
public class EventTaskSchedulerAsyncService {

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");

  @Getter(PRIVATE)
  private final EmailProperties emailProperties;

  private final ScheduleHandlerService scheduleHandlerService;

  private final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;

  private final CollectionTypeCodeRepository collectionTypeCodeRepository;

  private final CloseCollectionService closeCollectionService;

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @Getter(PRIVATE)
  private final SagaRepository sagaRepository;

  @Getter(PRIVATE)
  private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;

  @Getter(PRIVATE)
  private final Map<String, Orchestrator> sagaOrchestrators = new HashMap<>();

  @Getter(PRIVATE)
  private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

  @Getter(PRIVATE)
  private final SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;

  @Getter(PRIVATE)
  private final RestUtils restUtils;

  @Getter(PRIVATE)
  private final CollectionRepository collectionRepository;

  @Setter
  private List<String> statusFilters;

  @Value("${number.students.process.saga}")
  private String numberOfStudentsToProcess;

  @Value("${number.school.coll.process.saga}")
  private String numberOfSchoolCollToProcess;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionLightRepository sdcSchoolCollectionLightRepository;
  private final SdcSchoolCollectionService sdcSchoolCollectionService;

  public EventTaskSchedulerAsyncService(final List<Orchestrator> orchestrators, EmailProperties emailProperties, ScheduleHandlerService scheduleHandlerService, CollectionCodeCriteriaRepository collectionCodeCriteriaRepository, CollectionTypeCodeRepository collectionTypeCodeRepository, CloseCollectionService closeCollectionService, final SagaRepository sagaRepository, final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository, SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService, SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService, RestUtils restUtils, CollectionRepository collectionRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionLightRepository sdcSchoolCollectionLightRepository, SdcSchoolCollectionService sdcSchoolCollectionService, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
    this.emailProperties = emailProperties;
    this.scheduleHandlerService = scheduleHandlerService;
    this.collectionCodeCriteriaRepository = collectionCodeCriteriaRepository;
    this.collectionTypeCodeRepository = collectionTypeCodeRepository;
    this.closeCollectionService = closeCollectionService;
    this.sagaRepository = sagaRepository;
    this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
    this.sdcSchoolCollectionStudentService = sdcSchoolCollectionStudentService;
    this.sdcSchoolCollectionHistoryService = sdcSchoolCollectionHistoryService;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionLightRepository = sdcSchoolCollectionLightRepository;
    this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    this.collectionRepository = collectionRepository;
    this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
    this.restUtils = restUtils;
    orchestrators.forEach(orchestrator -> this.sagaOrchestrators.put(orchestrator.getSagaName(), orchestrator));
  }

  @Async("processUncompletedSagasTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndProcessUncompletedSagas() {
    log.debug("Processing uncompleted sagas");
    final var sagas = this.getSagaRepository().findTop500ByStatusInOrderByCreateDate(this.getStatusFilters());
    log.debug("Found {} sagas to be retried", sagas.size());
    if (!sagas.isEmpty()) {
      this.processUncompletedSagas(sagas);
    }
  }

  private void processUncompletedSagas(final List<SdcSagaEntity> sagas) {
    for (val saga : sagas) {
      if (saga.getUpdateDate().isBefore(LocalDateTime.now().minusMinutes(2))
        && this.getSagaOrchestrators().containsKey(saga.getSagaName())) {
        try {
          this.setRetryCountAndLog(saga);
          this.getSagaOrchestrators().get(saga.getSagaName()).replaySaga(saga);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
          log.error("InterruptedException while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, ex);
        } catch (final IOException | TimeoutException e) {
          log.error("Exception while findAndProcessPendingSagaEvents :: for saga :: {} :: {}", saga, e);
        }
      }
    }
  }

  @Async("deleteMigrateStudentsTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteMigrateStudentSagaRecordsForProcessing() {
    log.debug("Deleting migrated student SAGAs");

    sagaRepository.deleteCompletedMigrationSagas();
  }

  @Async("processLoadedStudentsTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndPublishMigratedStudentRecordsForProcessing() {
    log.debug("Querying for migrated students to process");
    if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 400) { // at max there will be 100 parallel sagas.
      log.debug("Saga count is greater than 100, so not processing student records");
      return;
    }

    final var sdcSchoolStudentEntities = this.getSdcSchoolStudentRepository().findTopMigratedStudentForProcessing(Integer.parseInt(numberOfStudentsToProcess));
    log.debug("Found :: {}  records in migrated status", sdcSchoolStudentEntities.size());
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcSchoolStudentEntities.stream()
            .map(el -> {
              val sdcStudentSagaData = new SdcStudentSagaData();
              var school = this.restUtils.getSchoolBySchoolID(el.getSdcSchoolCollection().getSchoolID().toString());
              sdcStudentSagaData.setCollectionTypeCode(el.getSdcSchoolCollection().getCollectionEntity().getCollectionTypeCode());
              sdcStudentSagaData.setSchool(school.get());
              sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(el));
              return sdcStudentSagaData;
            }).toList();
    if (!sdcSchoolStudentEntities.isEmpty()) {
      this.getSdcSchoolCollectionStudentService().prepareAndSendMigratedSdcStudentsForFurtherProcessing(sdcStudentSagaDatas);
    }
  }

  @Async("processLoadedStudentsTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAndPublishLoadedStudentRecordsForProcessing() {
    log.debug("Querying for loaded students to process");
    if (this.getSagaRepository().countAllByStatusIn(this.getStatusFilters()) > 100) { // at max there will be 100 parallel sagas.
      log.debug("Saga count is greater than 100, so not processing student records");
      return;
    }
    final var sdcSchoolStudentEntities = this.getSdcSchoolStudentRepository().findTopLoadedStudentForProcessing(numberOfStudentsToProcess);
    log.debug("Found :: {}  records in loaded status", sdcSchoolStudentEntities.size());
    if (!sdcSchoolStudentEntities.isEmpty()) {
      this.getSdcSchoolCollectionStudentService().prepareAndSendSdcStudentsForFurtherProcessing(sdcSchoolStudentEntities);
    }else{
      log.debug("Querying for DEMOG_UPD students to process");
      final var sdcSchoolStudentDemogUpdEntities = this.getSdcSchoolStudentRepository().findStudentForDownstreamUpdate(numberOfStudentsToProcess);
      log.debug("Found :: {}  records in DEMOG_UPD status", sdcSchoolStudentDemogUpdEntities.size());
      if (!sdcSchoolStudentDemogUpdEntities.isEmpty()) {
        this.getSdcSchoolCollectionStudentService().prepareStudentsForDemogUpdate(sdcSchoolStudentDemogUpdEntities);
      }
    }
  }

  @Async("findSchoolCollectionsForSubmissionTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findSchoolCollectionsForSubmission() {
    final List<SdcSchoolCollectionsForAutoSubmit> schoolsForSubmission = sdcSchoolCollectionRepository.findSchoolCollectionsWithStudentsNotInLoadedStatus(numberOfSchoolCollToProcess);
    log.debug("Found :: {}  school collection entities for processing", schoolsForSubmission.size());
    final List<SdcSchoolCollectionLightEntity> updatedCollections = new ArrayList<>();
    if (!schoolsForSubmission.isEmpty()) {
      schoolsForSubmission.forEach(submission -> {
        var schoolCollectionEntity = sdcSchoolCollectionLightRepository.findBySdcSchoolCollectionID(UUID.fromString(submission.getSdcSchoolCollectionID())).get();
         if(submission.getErrorCount() == 0) {
            if (submission.getDupeCount() == 0) {
              schoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.SUBMITTED.getCode());
            } else {
              schoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.VERIFIED.getCode());
            }
          } else {
           schoolCollectionEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
          }
         updatedCollections.add(schoolCollectionEntity);
      });
      updatedCollections.forEach(sdcSchoolCollectionService::saveSdcSchoolCollectionLightWithHistory);
    }
  }

  @Async("findAllUnsubmittedIndependentSchoolsTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findAllUnsubmittedIndependentSchoolsInCurrentCollection() {
    final Optional<CollectionEntity> activeCollectionOptional = collectionRepository.findActiveCollection();
    CollectionEntity activeCollection = activeCollectionOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "activeCollection"));

    if (activeCollection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.JULY.getTypeCode())) {
      return;
    }

    final List<SdcSchoolCollectionEntity> sdcSchoolCollectionEntities = sdcSchoolCollectionRepository.findAllUnsubmittedIndependentSchoolsInCurrentCollection();
    log.info("Found :: {} independent schools which have not yet submitted.", sdcSchoolCollectionEntities.size());
    if (!sdcSchoolCollectionEntities.isEmpty()) {
      scheduleHandlerService.createAndStartUnsubmittedEmailSagas(sdcSchoolCollectionEntities);
    }
  }

  @Async("findModifiedSchoolsAndUpdateSdcSchoolCollectionTaskExecutor")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void findModifiedSchoolsAndUpdateSdcSchoolCollection() {
    final Optional<CollectionEntity> activeCollectionOptional = collectionRepository.findActiveCollection();
    CollectionEntity activeCollection = activeCollectionOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "activeCollection"));

    if (!activeCollection.getCollectionStatusCode().equals(CollectionStatus.INPROGRESS.getCode())) {
      return;
    }

    Optional<CollectionTypeCodeEntity> optionalCollectionTypeCodeEntity = this.collectionTypeCodeRepository.findByCollectionTypeCode(activeCollection.getCollectionTypeCode());
    CollectionTypeCodeEntity collectionTypeCodeEntity = optionalCollectionTypeCodeEntity.orElseThrow(() -> new EntityNotFoundException(CollectionTypeCodeEntity.class, "collectionTypeCode", activeCollection.getCollectionTypeCode()));
    List<CollectionCodeCriteriaEntity> collectionCodeCriteria = this.collectionCodeCriteriaRepository.findAllByCollectionTypeCodeEntityEquals(collectionTypeCodeEntity);
    final List<SchoolTombstone> schoolTombstonesFromCollectionCodeCriteria = closeCollectionService.getListOfSchoolIDsFromCriteria(collectionCodeCriteria);

    restUtils.populateSchoolMap();
    final List<SchoolTombstone> schoolTombstones = restUtils.getSchools();
    final List<SdcSchoolCollectionEntity> activeSchoolCollections = sdcSchoolCollectionRepository.findAllByCollectionEntityCollectionID(activeCollection.getCollectionID());

    List<SdcSchoolCollectionEntity> newSchoolCollections = findAddSchoolsAndUpdateSdcSchoolCollection(schoolTombstonesFromCollectionCodeCriteria, activeCollection, activeSchoolCollections);
    List<SdcSchoolCollectionEntity> closedSchoolCollections = findClosedSchoolsAndDeleteSdcCollection(schoolTombstones, activeSchoolCollections);

    if (CollectionUtils.isNotEmpty(newSchoolCollections)) {
      newSchoolCollections.stream().forEach(sdcSchoolCollectionEntity -> sdcSchoolCollectionEntity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(sdcSchoolCollectionEntity, sdcSchoolCollectionEntity.getUpdateUser())));
      sdcSchoolCollectionRepository.saveAll(newSchoolCollections);
    }
    if (CollectionUtils.isNotEmpty(closedSchoolCollections)) {
      closedSchoolCollections.stream().forEach(sdcSchoolCollectionEntity -> sdcSchoolCollectionService.deleteSdcCollection(sdcSchoolCollectionEntity.getSdcSchoolCollectionID()));
    }
  }

  private List<SdcSchoolCollectionEntity> findAddSchoolsAndUpdateSdcSchoolCollection(List<SchoolTombstone> schoolTombstones, CollectionEntity activeCollection, List<SdcSchoolCollectionEntity> activeSchoolCollections) {
    Set<UUID> existingSchoolIds = activeSchoolCollections.stream()
            .map(SdcSchoolCollectionEntity::getSchoolID).collect(Collectors.toSet());

    return schoolTombstones.stream()
            .filter(tombstone -> !existingSchoolIds.contains(UUID.fromString(tombstone.getSchoolId())))
            .filter(tombstone -> StringUtils.isBlank(tombstone.getClosedDate())
                    && StringUtils.isNotBlank(tombstone.getOpenedDate())
                    && LocalDateTime.parse(tombstone.getOpenedDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).isBefore(LocalDateTime.now()))
            .map(tombstone -> {
              SdcSchoolCollectionEntity newEntity = new SdcSchoolCollectionEntity();
              newEntity.setCollectionEntity(activeCollection);
              newEntity.setSchoolID(UUID.fromString(tombstone.getSchoolId()));
              newEntity.setSdcDistrictCollectionID(
                      sdcDistrictCollectionRepository
                              .findByDistrictIDAndCollectionEntityCollectionID(
                                      UUID.fromString(tombstone.getDistrictId()),
                                      activeCollection.getCollectionID()
                              )
                              .map(SdcDistrictCollectionEntity::getSdcDistrictCollectionID)
                              .orElse(null)
              );
              newEntity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode());
              newEntity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
              newEntity.setCreateDate(LocalDateTime.now());
              newEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
              newEntity.setUpdateDate(LocalDateTime.now());
              return newEntity;
            })
            .toList();
  }


  private List<SdcSchoolCollectionEntity> findClosedSchoolsAndDeleteSdcCollection(List<SchoolTombstone> schoolTombstones, List<SdcSchoolCollectionEntity> activeSchoolCollections) {
    Set<UUID> closedSchoolIds = schoolTombstones.stream()
            .filter(tombstone -> StringUtils.isNotBlank(tombstone.getClosedDate())
                    && LocalDateTime.parse(tombstone.getClosedDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME).isBefore(LocalDateTime.now()))
            .map(SchoolTombstone::getSchoolId)
            .map(UUID::fromString).collect(Collectors.toSet());

    return activeSchoolCollections.stream()
            .filter(sdcSchoolCollectionEntity -> closedSchoolIds.contains(sdcSchoolCollectionEntity.getSchoolID()))
            .toList();
  }

  public List<String> getStatusFilters() {
    if (this.statusFilters != null && !this.statusFilters.isEmpty()) {
      return this.statusFilters;
    } else {
      final var statuses = new ArrayList<String>();
      statuses.add(SagaStatusEnum.IN_PROGRESS.toString());
      statuses.add(SagaStatusEnum.STARTED.toString());
      return statuses;
    }
  }

  private void setRetryCountAndLog(final SdcSagaEntity saga) {
    Integer retryCount = saga.getRetryCount();
    if (retryCount == null || retryCount == 0) {
      retryCount = 1;
    } else {
      retryCount += 1;
    }
    saga.setRetryCount(retryCount);
    this.getSagaRepository().save(saga);
    LogHelper.logSagaRetry(saga);
  }
}
