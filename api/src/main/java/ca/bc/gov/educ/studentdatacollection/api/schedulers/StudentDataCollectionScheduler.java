package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeCriteriaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.StartSDCCollectionsWithOpenDateInThePastProcessingHandler;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
public class StudentDataCollectionScheduler {
  private final CollectionTypeCodeRepository collectionCodeRepository;

  private final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;

  private final StartSDCCollectionsWithOpenDateInThePastProcessingHandler startSDCCollectionsWithOpenDateInThePastProcessingHandler;

  private final RestUtils restUtils;

  public StudentDataCollectionScheduler(final CollectionTypeCodeRepository collectionCodeRespository, final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository, final StartSDCCollectionsWithOpenDateInThePastProcessingHandler startSDCCollectionsWithOpenDateInThePastProcessingHandler, final RestUtils restUtils) {
    this.collectionCodeRepository = collectionCodeRespository;
    this.collectionCodeCriteriaRepository = collectionCodeCriteriaRepository;
    this.startSDCCollectionsWithOpenDateInThePastProcessingHandler = startSDCCollectionsWithOpenDateInThePastProcessingHandler;
    this.restUtils = restUtils;
  }

  @Scheduled(cron = "${cron.scheduled.process.events.start.collection}") //runs at midnight
  @SchedulerLock(name = "startSDCCollectionsWithOpenDateInThePast",
      lockAtLeastFor = "${cron.scheduled.process.events.start.collection.lockAtLeastFor}", lockAtMostFor = "${cron.scheduled.process.events.start.collection.lockAtMostFor}")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollectionsWithOpenDateInThePast() {
    log.info("startSDCCollectionsWithOpenDateInThePast :: running scheduler for open collections");
    LocalDateTime today = LocalDateTime.now();
    List<CollectionTypeCodeEntity> collectionsToOpen = this.collectionCodeRepository.findAllByOpenDateBefore(today);

    if(!collectionsToOpen.isEmpty()) {
      for (var collection : collectionsToOpen) {
        log.info("collectionCode {} needs to be open, obtaining collectionCodeCriteria", collection.getCollectionTypeCode());
        List<CollectionCodeCriteriaEntity> collectionCodeCriteria = this.collectionCodeCriteriaRepository.findAllByCollectionTypeCodeEntityEquals(collection);
        final List<String> listOfSchoolIDs = this.getListOfSchoolIDsFromCriteria(collectionCodeCriteria);

        if (!listOfSchoolIDs.isEmpty()) {
          log.info("processing {} schools to add to collection {}", listOfSchoolIDs.size(), collection.getCollectionTypeCode());
          this.startSDCCollectionsWithOpenDateInThePastProcessingHandler.startSDCCollection(collection, listOfSchoolIDs);
        }
      }
    }
  }

  private List<String> getListOfSchoolIDsFromCriteria(List<CollectionCodeCriteriaEntity> collectionCodeCriteria) {
    List<String> listOfSchoolIDs = new ArrayList<>();

    if(!collectionCodeCriteria.isEmpty()) {
      log.debug("found {} collectionCodeCriteria", collectionCodeCriteria.size());
      log.trace("CollectionCodeCriteria are {}", collectionCodeCriteria);
      final var results = this.restUtils.getSchoolListGivenCriteria(collectionCodeCriteria, UUID.randomUUID());
      log.info("found {} schools associated to collection", results.size());

      listOfSchoolIDs = results.stream().map(School::getSchoolId).toList();
    }

    return listOfSchoolIDs;
  }
}
