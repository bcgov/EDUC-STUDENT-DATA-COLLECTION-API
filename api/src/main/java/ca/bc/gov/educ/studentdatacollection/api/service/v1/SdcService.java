package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;


@Component
@Service
@Slf4j
public class SdcService {
  private final MessagePublisher messagePublisher;
  private final SdcSchoolCollectionStudentRepository repository;
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final SdcSchoolCollectionHistoryService sdcSchoolHistoryService;
  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public SdcService(final MessagePublisher messagePublisher, SdcSchoolCollectionStudentRepository repository, CollectionRepository collectionRepository, SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository, CollectionTypeCodeRepository collectionCodeRepository, SdcSchoolCollectionHistoryService sdcSchoolHistoryService) {
    this.messagePublisher = messagePublisher;
    this.repository = repository;
    this.collectionRepository = collectionRepository;
    this.sdcStudentValidationErrorRepository = sdcStudentValidationErrorRepository;
    this.collectionCodeRepository = collectionCodeRepository;
    this.sdcSchoolHistoryService = sdcSchoolHistoryService;
  }

  /**
   * Find all completable future.
   *
   * @param studentSpecs the student specs
   * @param pageNumber   the page number
   * @param pageSize     the page size
   * @param sorts        the sorts
   * @return the completable future
   */
  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<SdcSchoolCollectionStudentEntity>> findAll(final Specification<SdcSchoolCollectionStudentEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    return CompletableFuture.supplyAsync(() -> {
      final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        return this.repository.findAll(studentSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    }, this.paginatedQueryExecutor);

  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
  }

  @Async("publisherExecutor")
  public void prepareAndSendSdcStudentsForFurtherProcessing(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcStudentEntities.stream()
      .map(el -> {
        val sdcStudentSagaData = new SdcStudentSagaData();
        sdcStudentSagaData.setSchoolID(el.getSdcSchoolCollectionID().toString());
        sdcStudentSagaData.setSdcSchoolCollectionStudent(SdcSchoolCollectionStudentMapper.mapper.toSdcSchoolStudent(el));
        return sdcStudentSagaData;
      }).toList();
    this.publishUnprocessedStudentRecordsForProcessing(sdcStudentSagaDatas);
  }

  /**
   * Send individual student as message to topic consumer.
   */
  private void sendIndividualStudentAsMessageToTopic(final SdcStudentSagaData sdcStudentSagaData) {
    final var eventPayload = JsonUtil.getJsonString(sdcStudentSagaData);
    if (eventPayload.isPresent()) {
      final Event event = Event.builder().eventType(EventType.READ_FROM_TOPIC).eventOutcome(EventOutcome.READ_FROM_TOPIC_SUCCESS).eventPayload(eventPayload.get()).sdcSchoolStudentID(sdcStudentSagaData.getSdcSchoolCollectionStudent().getSdcSchoolCollectionStudentID()).build();
      final var eventString = JsonUtil.getJsonString(event);
      if (eventString.isPresent()) {
        this.messagePublisher.dispatchMessage(TopicsEnum.STUDENT_DATA_COLLECTION_API_TOPIC.toString(), eventString.get().getBytes());
      } else {
        log.error("Event String is empty, skipping the publish to topic :: {}", sdcStudentSagaData);
      }
    } else {
      log.error("Event payload is empty, skipping the publish to topic :: {}", sdcStudentSagaData);
    }
  }

  public Optional<SdcSchoolCollectionStudentEntity> findBySdcSchoolStudentID(final String sdcSchoolStudentID) {
    return this.repository.findById(UUID.fromString(sdcSchoolStudentID));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcStudentValidationErrors(final String sdcSchoolStudentID) {
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(UUID.fromString(sdcSchoolStudentID));
  }

  //To save NominalRollStudent with ValidationErrors, query and save operation should be in the same transaction boundary.
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public SdcSchoolCollectionStudentEntity saveSdcSchoolStudentValidationErrors(final String sdcSchoolCollectionStudentID, final List<SdcSchoolCollectionStudentValidationIssue> issues, SdcSchoolCollectionStudentEntity entity) {
    if(entity == null) {
      val sdcSchoolCollectionStudent = this.findBySdcSchoolStudentID(sdcSchoolCollectionStudentID);
      if (sdcSchoolCollectionStudent.isPresent()) {
        entity = sdcSchoolCollectionStudent.get();
      }else{
        throw new StudentDataCollectionAPIRuntimeException("Error while saving SDC school student with ValidationErrors - entity was null");
      }
    }
    entity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(issues, entity));
    entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
    return this.repository.save(entity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollection(CollectionTypeCodeEntity collectionCode, List<String> listOfSchoolIDs) {
    CollectionEntity collectionEntity = CollectionEntity.builder()
      .collectionTypeCode(collectionCode.getCollectionTypeCode())
      .openDate(collectionCode.getOpenDate())
      .closeDate(collectionCode.getCloseDate())
      .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .createDate(LocalDateTime.now())
      .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .updateDate(LocalDateTime.now()).build();

    CollectionEntity savedCollection = this.collectionRepository.save(collectionEntity);
    log.info("Collection created and saved");

    Set<SdcSchoolCollectionEntity> sdcSchoolEntityList = new HashSet<>();
    for(String schoolID : listOfSchoolIDs) {
      SdcSchoolCollectionEntity sdcSchoolEntity = SdcSchoolCollectionEntity.builder().collectionEntity(savedCollection)
        .schoolID(UUID.fromString(schoolID))
        .sdcSchoolCollectionStatusCode("NEW")
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();
      sdcSchoolEntityList.add(sdcSchoolEntity);
    }

    savedCollection.setSdcSchoolCollectionEntities(sdcSchoolEntityList);

    CollectionEntity savedCollectionWithSchoolEntities = this.collectionRepository.save(savedCollection);
    log.info("Collection saved with sdc school entities");

    if(!savedCollectionWithSchoolEntities.getSDCSchoolEntities().isEmpty()) {
      for (SdcSchoolCollectionEntity sdcSchoolEntity : savedCollectionWithSchoolEntities.getSDCSchoolEntities() ) {
        this.sdcSchoolHistoryService.createSDCSchoolHistory(sdcSchoolEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      }
    }

    collectionCode.setOpenDate(collectionCode.getOpenDate().plusYears(1));
    collectionCode.setCloseDate(collectionCode.getCloseDate().plusYears(1));
    collectionCode.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    collectionCode.setUpdateDate(LocalDateTime.now());

    CollectionTypeCodeEntity savedCollectionCode = this.collectionCodeRepository.save(collectionCode);
    log.info("Collection {} started, next open date is {}, next close date is {}", savedCollectionCode.getCollectionTypeCode(), savedCollectionCode.getOpenDate(), savedCollectionCode.getCloseDate());
  }


}
