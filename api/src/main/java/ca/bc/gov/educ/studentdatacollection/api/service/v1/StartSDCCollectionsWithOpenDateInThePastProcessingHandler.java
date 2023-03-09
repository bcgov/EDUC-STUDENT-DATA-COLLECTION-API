package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class StartSDCCollectionsWithOpenDateInThePastProcessingHandler {

  private final CollectionRepository collectionRepository;
  private final CollectionCodeRepository collectionCodeRepository;

  private final SdcSchoolHistoryService sdcSchoolHistoryService;

  @Autowired
  public StartSDCCollectionsWithOpenDateInThePastProcessingHandler(
      CollectionRepository collectionRepository, CollectionCodeRepository collectionCodeRepository, SdcSchoolHistoryService sdcSchoolHistoryService) {
    this.collectionRepository = collectionRepository;
    this.collectionCodeRepository = collectionCodeRepository;

    this.sdcSchoolHistoryService = sdcSchoolHistoryService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startSDCCollection(CollectionCodeEntity collectionCode, List<String> listOfSchoolIDs) {

    CollectionEntity collectionEntity = CollectionEntity.builder()
        .collectionCode(collectionCode.getCollectionCode())
        .openDate(collectionCode.getOpenDate())
        .closeDate(collectionCode.getCloseDate())
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();

    CollectionEntity savedCollection = this.collectionRepository.save(collectionEntity);
    log.info("Collection created and saved");

    Set<SdcSchoolEntity> sdcSchoolEntityList = new HashSet<>();
    for(String schoolID : listOfSchoolIDs) {
      SdcSchoolEntity sdcSchoolEntity = SdcSchoolEntity.builder().collectionEntity(savedCollection)
          .schoolID(UUID.fromString(schoolID))
          .collectionStatusTypeCode("NEW")
          .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
          .createDate(LocalDateTime.now())
          .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
          .updateDate(LocalDateTime.now()).build();
      sdcSchoolEntityList.add(sdcSchoolEntity);
    }

    savedCollection.setSdcSchoolEntities(sdcSchoolEntityList);

    CollectionEntity savedCollectionWithSchoolEntities = this.collectionRepository.save(savedCollection);
    log.info("Collection saved with sdc school entities");

    if(!savedCollectionWithSchoolEntities.getSDCSchoolEntities().isEmpty()) {
      for (SdcSchoolEntity sdcSchoolEntity : savedCollectionWithSchoolEntities.getSDCSchoolEntities() ) {
        this.sdcSchoolHistoryService.createSDCSchoolHistory(sdcSchoolEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      }
    }

    collectionCode.setOpenDate(collectionCode.getOpenDate().plusYears(1));
    collectionCode.setCloseDate(collectionCode.getCloseDate().plusYears(1));
    collectionCode.setUpdateDate(LocalDateTime.now());
    CollectionCodeEntity savedCollectionCode = this.collectionCodeRepository.save(collectionCode);
    log.info("Collection {} started, next open date is {}, next close date is {}", savedCollectionCode.getCollectionCode(), savedCollectionCode.getOpenDate(), savedCollectionCode.getCloseDate());

    }
}

