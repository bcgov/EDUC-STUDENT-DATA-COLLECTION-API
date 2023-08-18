package ca.bc.gov.educ.studentdatacollection.api.schedulers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeCriteriaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeCriteriaRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class StudentDataCollectionScheduler {
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
  private final SdcService sdcService;
  private final RestUtils restUtils;

  public StudentDataCollectionScheduler(final CollectionTypeCodeRepository collectionCodeRespository, final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository, SdcService sdcService, final RestUtils restUtils) {
    this.collectionCodeRepository = collectionCodeRespository;
    this.collectionCodeCriteriaRepository = collectionCodeCriteriaRepository;
    this.sdcService = sdcService;
    this.restUtils = restUtils;
  }

  @Scheduled(cron = "${cron.scheduled.process.events.start.collection}") //runs at midnight
  @SchedulerLock(name = "startSDCCollectionsWithOpenDateInThePast",
      lockAtLeastFor = "${cron.scheduled.process.events.start.collection.lockAtLeastFor}", lockAtMostFor = "${cron.scheduled.process.events.start.collection.lockAtMostFor}")
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollectionsWithOpenDateInThePast() {
    log.info("startSDCCollectionsWithOpenDateInThePast :: running scheduler for open collections");
    LocalDateTime today = LocalDateTime.now();
    List<CollectionTypeCodeEntity> collectionsToOpen = this.collectionCodeRepository.findAllByOpenDateBeforeAndEffectiveDateLessThanAndExpiryDateGreaterThan(today, today, today);

    if(!collectionsToOpen.isEmpty()) {
      for (var collection : collectionsToOpen) {
        log.info("collectionCode {} needs to be open, obtaining collectionCodeCriteria", collection.getCollectionTypeCode());
        List<CollectionCodeCriteriaEntity> collectionCodeCriteria = this.collectionCodeCriteriaRepository.findAllByCollectionTypeCodeEntityEquals(collection);
        final List<School> listOfSchoolIDs = this.getListOfSchoolIDsFromCriteria(collectionCodeCriteria);

        if (!listOfSchoolIDs.isEmpty()) {
          log.info("processing {} schools to add to collection {}", listOfSchoolIDs.size(), collection.getCollectionTypeCode());
          this.sdcService.startSDCCollection(collection, listOfSchoolIDs);
        }
      }
    }
  }

  private List<School> getListOfSchoolIDsFromCriteria(List<CollectionCodeCriteriaEntity> collectionCodeCriteria) {
    List<School> listOfSchoolIDs = new ArrayList<>();

    if(!collectionCodeCriteria.isEmpty()) {
      log.debug("found {} collectionCodeCriteria", collectionCodeCriteria.size());
      log.trace("CollectionCodeCriteria are {}", collectionCodeCriteria);
      listOfSchoolIDs = this.restUtils.getSchoolListGivenCriteria(collectionCodeCriteria, UUID.randomUUID());
      log.info("found {} schools associated to collection", listOfSchoolIDs.size());
    }

    return listOfSchoolIDs;
  }
}
