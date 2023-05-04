package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;


@Component
@Service
@Slf4j
@RequiredArgsConstructor
public class SdcService {

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final CollectionRepository collectionRepository;
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final SdcSchoolCollectionHistoryService sdcSchoolHistoryService;

  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

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
        return this.sdcSchoolCollectionStudentRepository.findAll(studentSpecs, paging);
      } catch (final Exception ex) {
        throw new CompletionException(ex);
      }
    }, this.paginatedQueryExecutor);

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
