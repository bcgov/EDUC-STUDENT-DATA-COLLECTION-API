package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcDistrictCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executor;


@Component
@Service
@Slf4j
@RequiredArgsConstructor
public class SdcService {

  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final CollectionRepository collectionRepository;
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final SdcSchoolCollectionHistoryService sdcSchoolHistoryService;

  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollection(CollectionTypeCodeEntity collectionCode, List<SchoolTombstone> listOfSchoolTombstones) {
    CollectionEntity collectionEntity = CollectionEntity.builder()
      .collectionTypeCode(collectionCode.getCollectionTypeCode())
      .collectionStatusCode(CollectionStatus.INPROGRESS.getCode())
      .openDate(collectionCode.getOpenDate())
      .closeDate(collectionCode.getCloseDate())
      .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .createDate(LocalDateTime.now())
      .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .updateDate(LocalDateTime.now()).build();

    collectionRepository.save(collectionEntity);

    var sdcDistrictEntityList = new HashMap<UUID, SdcDistrictCollectionEntity>();
    var listOfDistricts = listOfSchoolTombstones.stream().map(SchoolTombstone::getDistrictId).distinct().toList();
    listOfDistricts.forEach(districtID -> {
      if(!sdcDistrictEntityList.containsKey(UUID.fromString(districtID))) {
        SdcDistrictCollectionEntity sdcDistrictCollectionEntity = SdcDistrictCollectionEntity.builder().collectionEntity(collectionEntity)
                .districtID(UUID.fromString(districtID))
                .sdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.NEW.getCode())
                .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .createDate(LocalDateTime.now())
                .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                .updateDate(LocalDateTime.now()).build();
        sdcDistrictCollectionEntity = sdcDistrictCollectionRepository.save(sdcDistrictCollectionEntity);
        sdcDistrictEntityList.put(sdcDistrictCollectionEntity.getDistrictID(), sdcDistrictCollectionEntity);
      }
    });

    Set<SdcSchoolCollectionEntity> sdcSchoolEntityList = new HashSet<>();
    listOfSchoolTombstones.forEach(school -> {

      UUID sdcDistrictCollectionID = null;
      if(SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())){
        sdcDistrictCollectionID = sdcDistrictEntityList.get(UUID.fromString(school.getDistrictId())).getSdcDistrictCollectionID();
      }

      SdcSchoolCollectionEntity sdcSchoolEntity = SdcSchoolCollectionEntity.builder().collectionEntity(collectionEntity)
        .schoolID(UUID.fromString(school.getSchoolId()))
        .sdcDistrictCollectionID(sdcDistrictCollectionID)
        .sdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.NEW.getCode())
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();
      sdcSchoolEntityList.add(sdcSchoolEntity);
    });

    collectionEntity.setSdcSchoolCollectionEntities(sdcSchoolEntityList);

    if(!collectionEntity.getSDCSchoolEntities().isEmpty()) {
      for (SdcSchoolCollectionEntity sdcSchoolEntity : collectionEntity.getSDCSchoolEntities() ) {
        sdcSchoolEntity.getSdcSchoolCollectionHistoryEntities().add(this.sdcSchoolHistoryService.createSDCSchoolHistory(sdcSchoolEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API));
      }
    }

    this.collectionRepository.save(collectionEntity);

    collectionCode.setOpenDate(collectionCode.getOpenDate().plusYears(1));
    collectionCode.setCloseDate(collectionCode.getCloseDate().plusYears(1));
    collectionCode.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    collectionCode.setUpdateDate(LocalDateTime.now());

    CollectionTypeCodeEntity savedCollectionCode = this.collectionCodeRepository.save(collectionCode);
    log.debug("Collection {} started, next open date is {}, next close date is {}", savedCollectionCode.getCollectionTypeCode(), savedCollectionCode.getOpenDate(), savedCollectionCode.getCloseDate());
  }
}
