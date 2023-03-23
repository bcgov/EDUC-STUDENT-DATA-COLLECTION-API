package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome;
import ca.bc.gov.educ.studentdatacollection.api.constants.EventType;
import ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolStudentStatus;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.SdcHelper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentValidationIssueEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentValidationIssueRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;


@Component
@Service
@Slf4j
public class SdcService {
  private static final String STUDENT_ID_ATTRIBUTE = "nominalRollStudentID";
  private final MessagePublisher messagePublisher;
  private final SdcSchoolCollectionStudentRepository repository;
  private final SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository;
  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public SdcService(final MessagePublisher messagePublisher, SdcSchoolCollectionStudentRepository repository, SdcSchoolCollectionStudentValidationIssueRepository sdcStudentValidationErrorRepository) {
    this.messagePublisher = messagePublisher;
    this.repository = repository;
    this.sdcStudentValidationErrorRepository = sdcStudentValidationErrorRepository;
  }

  public boolean isAllRecordsProcessed() {
    final long count = this.repository.countByStatus(SdcSchoolStudentStatus.LOADED.toString());
    return count < 1;
  }


  public boolean hasDuplicateRecords(final String sdcSchoolBatchID) {
    final Long count = this.repository.countForDuplicateStudentPENs(sdcSchoolBatchID);
    return count != null && count > 1;
  }

  public SdcSchoolCollectionStudentEntity getSdcStudentByID(final UUID sdcSchoolStudentID) {
    return this.repository.findById(sdcSchoolStudentID).orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudentEntity.class, STUDENT_ID_ATTRIBUTE, sdcSchoolStudentID.toString()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void saveNominalRollStudents(final List<SdcSchoolCollectionStudentEntity> nomRollStudentEntities, final String correlationID) {
    log.debug("creating nominal roll entities in transient table for transaction ID :: {}", correlationID);
    this.repository.saveAll(nomRollStudentEntities);
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

  public SdcSchoolCollectionStudentEntity updateNominalRollStudent(final SdcSchoolCollectionStudentEntity entity) {
    return this.repository.save(entity);
  }

  public void publishUnprocessedStudentRecordsForProcessing(final List<SdcStudentSagaData> sdcStudentSagaDatas) {
    sdcStudentSagaDatas.forEach(this::sendIndividualStudentAsMessageToTopic);
  }

  @Async("publisherExecutor")
  public void prepareAndSendSdcStudentsForFurtherProcessing(final List<SdcSchoolCollectionStudentEntity> sdcStudentEntities) {
    final List<SdcStudentSagaData> sdcStudentSagaDatas = sdcStudentEntities.stream()
      .map(el -> {
        val sdcStudentSagaData = new SdcStudentSagaData();
        sdcStudentSagaData.setSchoolID(el.getSdcSchoolCollectionEntity().getSchoolID().toString());
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
  public void saveSdcSchoolStudent(final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    this.repository.save(sdcSchoolStudentEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteSdcStudentValidationErrors(final String sdcSchoolStudentID) {
    this.sdcStudentValidationErrorRepository.deleteSdcStudentValidationErrors(UUID.fromString(sdcSchoolStudentID));
  }

  //To save NominalRollStudent with ValidationErrors, query and save operation should be in the same transaction boundary.
  public SdcSchoolCollectionStudentEntity saveSdcSchoolStudentValidationErrors(final String nominalRollStudentID, final Map<String, String> errors, SdcSchoolCollectionStudentEntity entity) {
    if(entity == null) {
      val nomRollStudOptional = this.findBySdcSchoolStudentID(nominalRollStudentID);
      if (nomRollStudOptional.isPresent()) {
        entity = nomRollStudOptional.get();
      }else{
        throw new StudentDataCollectionAPIRuntimeException("Error while saving SDC school student with ValidationErrors - entity was null");
      }
    }
    entity.getSDCStudentValidationIssueEntities().addAll(SdcHelper.populateValidationErrors(errors, entity));
    entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.ERROR.toString());
    return this.repository.save(entity);
  }

  public List<SdcSchoolCollectionStudentEntity> findAllBySdcSchoolBatchID(final String sdcSchoolBatchID) {
    return this.repository.findAllBySdcSchoolBatchID(sdcSchoolBatchID);
  }

  public List<SdcSchoolCollectionStudentValidationIssueEntity> getSchoolNumberValidationErrors(){
    return this.sdcStudentValidationErrorRepository.findAllByFieldName("School Number");
  }

}
