package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class StartSDCCollectionsWithOpenDateInThePastProcessingHandler {

  private final SdcRepository collectionRepository;
  private final CollectionCodeRepository collectionCodeRepository;

  private final SdcSchoolHistoryService sdcSchoolHistoryService;

  @Autowired
  public StartSDCCollectionsWithOpenDateInThePastProcessingHandler(
    SdcRepository collectionRepository, CollectionCodeRepository collectionCodeRepository, SdcSchoolHistoryService sdcSchoolHistoryService) {
    this.collectionRepository = collectionRepository;
    this.collectionCodeRepository = collectionCodeRepository;

    this.sdcSchoolHistoryService = sdcSchoolHistoryService;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startSDCCollection(CollectionCodeEntity collectionCode, List<String> listOfSchoolIDs) {

    SdcEntity collectionEntity = SdcEntity.builder()
        .collectionCode(collectionCode.getCollectionCode())
        .openDate(collectionCode.getOpenDate())
        .closeDate(collectionCode.getCloseDate())
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();

    SdcEntity savedCollection = this.collectionRepository.save(collectionEntity);
    log.info("Collection created and saved");

    Set<SdcSchoolBatchEntity> sdcSchoolEntityList = new HashSet<>();
    for(String schoolID : listOfSchoolIDs) {
      SdcSchoolBatchEntity sdcSchoolEntity = SdcSchoolBatchEntity.builder().sdcEntity(savedCollection)
          .schoolID(UUID.fromString(schoolID))
          .statusCode("NEW")
          .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
          .createDate(LocalDateTime.now())
          .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
          .updateDate(LocalDateTime.now()).build();
      sdcSchoolEntityList.add(sdcSchoolEntity);
    }

    savedCollection.setSdcSchoolEntities(sdcSchoolEntityList);

    SdcEntity savedCollectionWithSchoolEntities = this.collectionRepository.save(savedCollection);
    log.info("Collection saved with sdc school entities");

    if(!savedCollectionWithSchoolEntities.getSDCSchoolEntities().isEmpty()) {
      for (SdcSchoolBatchEntity sdcSchoolEntity : savedCollectionWithSchoolEntities.getSDCSchoolEntities() ) {
        this.sdcSchoolHistoryService.createSDCSchoolHistory(sdcSchoolEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      }
    }

    collectionCode.setOpenDate(collectionCode.getOpenDate().plusYears(1));
    collectionCode.setCloseDate(collectionCode.getCloseDate().plusYears(1));
    collectionCode.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    collectionCode.setUpdateDate(LocalDateTime.now());

    CollectionCodeEntity savedCollectionCode = this.collectionCodeRepository.save(collectionCode);
    log.info("Collection {} started, next open date is {}, next close date is {}", savedCollectionCode.getCollectionCode(), savedCollectionCode.getOpenDate(), savedCollectionCode.getCloseDate());

    }
}

