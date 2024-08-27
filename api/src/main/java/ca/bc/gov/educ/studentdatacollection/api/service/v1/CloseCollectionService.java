package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.properties.EmailProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.CollectionSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.EmailSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService;
    private final SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository;
    private final SdcDuplicateRepository sdcDuplicateRepository;
    private final EmailService emailService;
    private final EmailProperties emailProperties;
    private static final String SDC_COLLECTION_ID_KEY = "collectionID";

    public CloseCollectionService(CollectionRepository collectionRepository, CollectionTypeCodeRepository collectionTypeCodeRepository, CollectionCodeCriteriaRepository collectionCodeCriteriaRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionHistoryService sdcSchoolHistoryService, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionHistoryService sdcSchoolCollectionHistoryService, SdcSchoolCollectionStudentHistoryRepository sdcSchoolCollectionStudentHistoryRepository, SdcDuplicateRepository sdcDuplicateRepository, EmailService emailService, EmailProperties emailProperties) {
        this.collectionRepository = collectionRepository;
        this.collectionTypeCodeRepository = collectionTypeCodeRepository;
        this.collectionCodeCriteriaRepository = collectionCodeCriteriaRepository;
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.sdcSchoolHistoryService = sdcSchoolHistoryService;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionHistoryService = sdcSchoolCollectionHistoryService;
        this.sdcSchoolCollectionStudentHistoryRepository = sdcSchoolCollectionStudentHistoryRepository;
        this.sdcDuplicateRepository = sdcDuplicateRepository;
        this.emailService = emailService;
        this.emailProperties = emailProperties;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void closeCurrentCollAndOpenNewCollection(final CollectionSagaData collectionSagaData) {
        Optional<CollectionEntity> entityOptional = collectionRepository.findById(UUID.fromString(collectionSagaData.getExistingCollectionID()));
        CollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, SDC_COLLECTION_ID_KEY, collectionSagaData.getExistingCollectionID()));

        //find school collections that are not COMPLETED
        List<SdcSchoolCollectionEntity> schoolCollectionEntities = sdcSchoolCollectionRepository.findUncompletedSchoolCollections(entity.getCollectionID());
        if(!schoolCollectionEntities.isEmpty()) {
            markSchoolCollectionsAsCompleted(schoolCollectionEntities);
        }

        //find district collections that are not COMPLETED
        List<SdcDistrictCollectionEntity> districtCollectionEntities = sdcDistrictCollectionRepository.findAllIncompleteDistrictCollections(entity.getCollectionID());
        if(!districtCollectionEntities.isEmpty()) {
            markDistrictCollectionsAsCompleted(districtCollectionEntities);
        }

        // mark existing collection as COMPLETED
        entity.setCollectionStatusCode(CollectionStatus.COMPLETED.getCode());
        entity.setCloseDate(LocalDateTime.now());
        entity.setUpdateDate(LocalDateTime.now());
        entity.setUpdateUser(collectionSagaData.getUpdateUser());
        collectionRepository.save(entity);
        log.debug("Current collection type {}, id {}, is now closed", entity.getCollectionTypeCode(), entity.getCollectionID());

        // get next collection type code to open
        Optional<CollectionTypeCodes> optionalCollectionMap = CollectionTypeCodes.findByValue(entity.getCollectionTypeCode());
        CollectionTypeCodes collectionMap= optionalCollectionMap.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, SDC_COLLECTION_ID_KEY, collectionSagaData.getExistingCollectionID()));
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
                    .openDate(LocalDateTime.of(LocalDate.now(), LocalTime.MIDNIGHT))
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

            sdcSchoolCollectionStudentRepository.updateAllSdcSchoolCollectionStudentStatus(UUID.fromString(collectionSagaData.getExistingCollectionID()));
            log.info("New collection for {} is now open", collectionEntity.getCollectionTypeCode());
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

    public void markSchoolCollectionsAsCompleted(List<SdcSchoolCollectionEntity> schoolCollectionEntities){
        schoolCollectionEntities.forEach(entity -> {
            this.sdcDuplicateRepository.deleteAllBySdcDuplicateStudentEntities_SdcSchoolCollectionID(entity.getSdcSchoolCollectionID());
            this.sdcSchoolCollectionStudentHistoryRepository.deleteBySdcSchoolCollectionStudentIDs(
                    entity.getSDCSchoolStudentEntities().stream().map(SdcSchoolCollectionStudentEntity::getSdcSchoolCollectionStudentID).toList()
            );
            this.sdcSchoolCollectionStudentRepository.deleteAll(entity.getSDCSchoolStudentEntities());
            entity.getSDCSchoolStudentEntities().clear();
            entity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.COMPLETED.getCode());
            entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
            entity.setUpdateDate(LocalDateTime.now());
            entity.setUploadFileName(null);
            entity.setUploadDate(null);
            entity.getSdcSchoolCollectionHistoryEntities().add(sdcSchoolCollectionHistoryService.createSDCSchoolHistory(entity, ApplicationProperties.STUDENT_DATA_COLLECTION_API));
            sdcSchoolCollectionRepository.save(entity);
        });
    }

    public void markDistrictCollectionsAsCompleted(List<SdcDistrictCollectionEntity> districtCollectionEntities) {
        districtCollectionEntities.forEach(entity -> {
            entity.setSdcDistrictCollectionStatusCode(SdcDistrictCollectionStatus.COMPLETED.getCode());
            entity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
            entity.setUpdateDate(LocalDateTime.now());
            sdcDistrictCollectionRepository.save(entity);
        });
    }

    public void sendClosureNotification(final CollectionSagaData collectionSagaData) {
        Optional<CollectionEntity> entityOptional = collectionRepository.findById(UUID.fromString(collectionSagaData.getExistingCollectionID()));
        CollectionEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, SDC_COLLECTION_ID_KEY, collectionSagaData.getExistingCollectionID()));

        var emailFields = new HashMap<String, String>();
        emailFields.put("closeCollectionMonth", entity.getCollectionTypeCode());
        emailFields.put("closeCollectionYear", String.valueOf(entity.getCloseDate().getYear()));

        var toEmail = Arrays.stream(emailProperties.getClosureNotificationTo().split(",")).toList();

        var emailSagaData = createEmailSagaData(emailProperties.getSchoolNotificationEmailFrom(), toEmail,
                emailProperties.getEmailSubjectClosureNotification(), "closure.report.notification", emailFields);

        this.emailService.sendEmail(emailSagaData);
    }

    private EmailSagaData createEmailSagaData(String fromEmail, List<String> emailList, String subject, String templateName, HashMap<String, String> emailFields){
        return EmailSagaData
                .builder()
                .fromEmail(fromEmail)
                .toEmails(emailList)
                .subject(subject)
                .templateName(templateName)
                .emailFields(emailFields)
                .build();
    }
}
