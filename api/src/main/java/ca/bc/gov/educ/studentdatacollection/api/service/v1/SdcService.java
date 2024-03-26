package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
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

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void startSDCCollection(CollectionTypeCodeEntity collectionCode, List<School> listOfSchools) {
    CollectionEntity collectionEntity = CollectionEntity.builder()
      .collectionTypeCode(collectionCode.getCollectionTypeCode())
      .openDate(collectionCode.getOpenDate())
      .closeDate(collectionCode.getCloseDate())
      .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .createDate(LocalDateTime.now())
      .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
      .updateDate(LocalDateTime.now()).build();

    Set<SdcSchoolCollectionEntity> sdcSchoolEntityList = new HashSet<>();
    Set<SdcDistrictCollectionEntity> sdcDistrictEntityList = new HashSet<>();
    listOfSchools.forEach(school -> {
      SdcSchoolCollectionEntity sdcSchoolEntity = SdcSchoolCollectionEntity.builder().collectionEntity(collectionEntity)
        .schoolID(UUID.fromString(school.getSchoolId()))
        .districtID(UUID.fromString(school.getDistrictId()))
        .sdcSchoolCollectionStatusCode("NEW")
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();
      sdcSchoolEntityList.add(sdcSchoolEntity);
      SdcDistrictCollectionEntity sdcDistrictEntity = SdcDistrictCollectionEntity.builder().collectionEntity(collectionEntity)
        .districtID(UUID.fromString(school.getDistrictId()))
        .sdcDistrictCollectionStatusCode("NEW")
        .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .createDate(LocalDateTime.now())
        .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
        .updateDate(LocalDateTime.now()).build();
      sdcDistrictEntityList.add(sdcDistrictEntity);
    });

    collectionEntity.setSdcSchoolCollectionEntities(sdcSchoolEntityList);
    collectionEntity.setSdcDistrictCollectionEntities(sdcDistrictEntityList);

    if(!collectionEntity.getSDCSchoolEntities().isEmpty()) {
      for (SdcSchoolCollectionEntity sdcSchoolEntity : collectionEntity.getSDCSchoolEntities() ) {
        sdcSchoolEntity.getSdcSchoolCollectionHistoryEntities().add(this.sdcSchoolHistoryService.createSDCSchoolHistory(sdcSchoolEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API));
      }
    }

    this.collectionRepository.save(collectionEntity);
    log.info("Collection saved with entities");

    collectionCode.setOpenDate(collectionCode.getOpenDate().plusYears(1));
    collectionCode.setCloseDate(collectionCode.getCloseDate().plusYears(1));
    collectionCode.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    collectionCode.setUpdateDate(LocalDateTime.now());

    CollectionTypeCodeEntity savedCollectionCode = this.collectionCodeRepository.save(collectionCode);
    log.info("Collection {} started, next open date is {}, next close date is {}", savedCollectionCode.getCollectionTypeCode(), savedCollectionCode.getOpenDate(), savedCollectionCode.getCloseDate());
  }
}
