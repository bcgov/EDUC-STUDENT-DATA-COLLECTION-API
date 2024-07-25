package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class CloseCollectionService {

    private final CollectionRepository collectionRepository;
    private final CollectionTypeCodeRepository collectionTypeCodeRepository;
    private final CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionHistoryService sdcSchoolHistoryService;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;

    public CloseCollectionService(CollectionRepository collectionRepository, CollectionTypeCodeRepository collectionTypeCodeRepository, CollectionCodeCriteriaRepository collectionCodeCriteriaRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionHistoryService sdcSchoolHistoryService, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        this.collectionRepository = collectionRepository;
        this.collectionTypeCodeRepository = collectionTypeCodeRepository;
        this.collectionCodeCriteriaRepository = collectionCodeCriteriaRepository;
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.sdcSchoolHistoryService = sdcSchoolHistoryService;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeCurrentCollAndOpenNewCollection(final CollectionSagaData collectionSagaData) {
        Optional<CollectionEntity> entityOptional = collectionRepository.findById(UUID.fromString(collectionSagaData.getExistingCollectionID()));
        CollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionSagaData.getExistingCollectionID()));

        // mark existing collection as COMPLETED
        entity.setCollectionStatusCode(CollectionStatus.COMPLETED.getCode());
        entity.setCloseDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        entity.setUpdateUser(collectionSagaData.getUpdateUser());
        collectionRepository.save(entity);
        log.debug("Current collection type {}, id {}, is now closed", entity.getCollectionTypeCode(), entity.getCollectionID());

        // get next collection type code to open
        Optional<CollectionTypeCodes> optionalCollectionMap = CollectionTypeCodes.findByValue(entity.getCollectionTypeCode());
        CollectionTypeCodes collectionMap= optionalCollectionMap.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "collectionID", collectionSagaData.getExistingCollectionID()));
        log.debug("Next collection to open: {}", collectionMap.getNextCollectionToOpen());

        // get next collection entity
        Optional<CollectionTypeCodeEntity> optionalCollectionToOpen = this.collectionTypeCodeRepository.findByCollectionTypeCode(collectionMap.getNextCollectionToOpen());
        CollectionTypeCodeEntity collectionToOpen = optionalCollectionToOpen.orElseThrow(() -> new EntityNotFoundException(CollectionTypeCodeEntity.class, "collectionTypeCode", collectionMap.getNextCollectionToOpen()));

        // get next collection code criteria to populate schools and district
        List<CollectionCodeCriteriaEntity> collectionCodeCriteria = this.collectionCodeCriteriaRepository.findAllByCollectionTypeCodeEntityEquals(collectionToOpen);
        log.debug("Found {} collectionCodeCriteria", collectionCodeCriteria.size());

        final List<SchoolTombstone> listOfSchoolIDs = this.getListOfSchoolIDsFromCriteria(collectionCodeCriteria);
        log.debug("Found {} listOfSchoolIDs to open for next collection", listOfSchoolIDs.size());
        if (!listOfSchoolIDs.isEmpty()) {
            // create new collection
            CollectionEntity collectionEntity = CollectionEntity.builder()
                    .collectionTypeCode(collectionToOpen.getCollectionTypeCode())
                    .collectionStatusCode(CollectionStatus.INPROGRESS.getCode())
                    .openDate(LocalDateTime.now())
                    .closeDate(collectionToOpen.getCloseDate())
                    .snapshotDate(LocalDate.parse(collectionSagaData.getNewCollectionSnapshotDate()))
                    .submissionDueDate(LocalDate.parse(collectionSagaData.getNewCollectionSubmissionDueDate()))
                    .duplicationResolutionDueDate(LocalDate.parse(collectionSagaData.getNewCollectionDuplicationResolutionDueDate()))
                    .signOffDueDate(LocalDate.parse(collectionSagaData.getNewCollectionSignOffDueDate()))
                    .createUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                    .createDate(LocalDateTime.now())
                    .updateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API)
                    .updateDate(LocalDateTime.now()).build();

            collectionRepository.save(collectionEntity);
            startSDCCollection(listOfSchoolIDs, collectionEntity);

            // update the collection type code table
            collectionToOpen.setCloseDate(collectionToOpen.getCloseDate().plusYears(1));
            collectionToOpen.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
            collectionToOpen.setUpdateDate(LocalDateTime.now());
            collectionTypeCodeRepository.save(collectionToOpen);

            log.info("New collection for {} is now open", collectionToOpen.getCollectionTypeCode());

            List<SdcSchoolCollectionStudentEntity> studentsInCollections=  sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_CollectionEntity_CollectionID(UUID.fromString(collectionSagaData.getExistingCollectionID()));
            log.debug("Found {} studentsInCollections for downstream update", studentsInCollections.size());
            studentsInCollections.forEach(student -> {
                student.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.DEMOG_UPD.getCode());
                student.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
                student.setUpdateDate(LocalDateTime.now());
            });
            sdcSchoolCollectionStudentRepository.saveAll(studentsInCollections);
        }
    }

    public void startSDCCollection(List<SchoolTombstone> listOfSchoolTombstones, CollectionEntity collectionEntity) {
        var sdcDistrictEntityList = new HashMap<UUID, SdcDistrictCollectionEntity>();
        var listOfDistricts = listOfSchoolTombstones.stream().map(SchoolTombstone::getDistrictId).distinct().toList();
        log.debug("Found {} listOfDistricts to open for next collection", listOfDistricts.size());
        //create and save district collection entities
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

        //create school collection entities
        Set<SdcSchoolCollectionEntity> sdcSchoolEntityList = new HashSet<>();
        listOfSchoolTombstones.forEach(school -> {
            UUID sdcDistrictCollectionID = null;
            if(!SchoolCategoryCodes.INDEPENDENTS_AND_OFFSHORE.contains(school.getSchoolCategoryCode())){
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
            log.debug("Adding school history records for collection {}", collectionEntity.getCollectionID());
            collectionEntity.getSDCSchoolEntities().forEach(schoolCollectionEntity -> schoolCollectionEntity.getSdcSchoolCollectionHistoryEntities().add(this.sdcSchoolHistoryService.createSDCSchoolHistory(schoolCollectionEntity, ApplicationProperties.STUDENT_DATA_COLLECTION_API)));
        }

        //save school collection entities to collection
        this.collectionRepository.save(collectionEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markStudentAsCompleted(UpdateStudentSagaData updateStudentSagaData) {
        Optional<SdcSchoolCollectionStudentEntity> optionalStudentEntity =  sdcSchoolCollectionStudentRepository.findById(UUID.fromString(updateStudentSagaData.getSdcSchoolCollectionStudentID()));
        SdcSchoolCollectionStudentEntity studentEntity = optionalStudentEntity.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, "sdcSchoolCollectionStudentID", updateStudentSagaData.getSdcSchoolCollectionStudentID()));
        studentEntity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolStudentStatus.COMPLETED.getCode());
        studentEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
        studentEntity.setUpdateDate(LocalDateTime.now());

        sdcSchoolCollectionStudentRepository.save(studentEntity);
    }

    private List<SchoolTombstone> getListOfSchoolIDsFromCriteria(List<CollectionCodeCriteriaEntity> collectionCodeCriteria) {
        List<SchoolTombstone> listOfSchoolIDs = new ArrayList<>();
        if(!collectionCodeCriteria.isEmpty()) {
            listOfSchoolIDs = this.restUtils.getSchoolListGivenCriteria(collectionCodeCriteria, UUID.randomUUID());
            log.debug("found {} schools associated to collection", listOfSchoolIDs.size());
        }
        return listOfSchoolIDs;
    }
}