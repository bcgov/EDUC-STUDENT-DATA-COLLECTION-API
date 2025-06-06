package ca.bc.gov.educ.studentdatacollection.api.orchestrator;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CloseCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SagaService;
import ca.bc.gov.educ.studentdatacollection.api.struct.Event;
import ca.bc.gov.educ.studentdatacollection.api.struct.UpdateStudentSagaData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1.Student;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.UPDATE_SDC_STUDENT_STATUS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.EventType.UPDATE_STUDENT;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA;
import static ca.bc.gov.educ.studentdatacollection.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.STUDENT_API_TOPIC;
import static ca.bc.gov.educ.studentdatacollection.api.constants.TopicsEnum.UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC;

@Component
@Slf4j
public class UpdateStudentDownstreamOrchestrator extends BaseOrchestrator<UpdateStudentSagaData> {

    private final CloseCollectionService closeCollectionService;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;

    protected UpdateStudentDownstreamOrchestrator(SagaService sagaService, MessagePublisher messagePublisher, CloseCollectionService closeCollectionService, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        super(sagaService, messagePublisher, UpdateStudentSagaData.class, UPDATE_STUDENT_DOWNSTREAM_SAGA.toString(), UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC.toString());
        this.closeCollectionService = closeCollectionService;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(UPDATE_STUDENT, this::updateStudent)
                .step(UPDATE_STUDENT, STUDENT_UPDATED, UPDATE_SDC_STUDENT_STATUS, this::updateSdcStudentEllAndStatus)
                .step(UPDATE_STUDENT, NO_STUDENT_UPDATE_NEEDED, UPDATE_SDC_STUDENT_STATUS, this::updateSdcStudentEllAndStatus)
                .end(UPDATE_SDC_STUDENT_STATUS, SDC_STUDENT_STATUS_UPDATED);
    }

    public void updateStudent(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_STUDENT.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Student studentDataFromEventResponse = this.restUtils.getStudentByPEN(UUID.randomUUID(), updateStudentSagaData.getAssignedPEN());
        final List<SdcSchoolCollectionStudentEntity> otherStudentsWithSameAssignedID =sdcSchoolCollectionStudentRepository.findAllDuplicatesForStudentInCollection(UUID.fromString(updateStudentSagaData.getCollectionID()), UUID.fromString(updateStudentSagaData.getAssignedStudentID()), UUID.fromString(updateStudentSagaData.getSdcSchoolCollectionStudentID()));


        if (CollectionUtils.isEmpty(otherStudentsWithSameAssignedID) || isStudentAttendingSchoolOfRecord(updateStudentSagaData, otherStudentsWithSameAssignedID)){

            studentDataFromEventResponse.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
            studentDataFromEventResponse.setMincode(updateStudentSagaData.getMincode());
            studentDataFromEventResponse.setLocalID(updateStudentSagaData.getLocalID());
            updateGradeYear(studentDataFromEventResponse, updateStudentSagaData);
            updateUsualNameFields(studentDataFromEventResponse, updateStudentSagaData);
            studentDataFromEventResponse.setPostalCode(updateStudentSagaData.getPostalCode());
            studentDataFromEventResponse.setHistoryActivityCode("SLD");

            final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                    .eventType(UPDATE_STUDENT)
                    .replyTo(this.getTopicToSubscribe())
                    .eventPayload(JsonUtil.getJsonStringFromObject(studentDataFromEventResponse))
                    .build();
            this.postMessageToTopic(STUDENT_API_TOPIC.toString(), nextEvent);
            log.debug("message sent to STUDENT_API_TOPIC for UPDATE_STUDENT Event.");

        } else {
            final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                    .eventType(UPDATE_STUDENT)
                    .eventOutcome(NO_STUDENT_UPDATE_NEEDED)
                    .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                    .build();
            this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
            log.debug("message sent to UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC for NO_STUDENT_UPDATE_NEEDED Event.");
        }
    }

    public boolean isStudentAttendingSchoolOfRecord(UpdateStudentSagaData currStudent, List<SdcSchoolCollectionStudentEntity> otherStudents){
        Float maxCourseNumberFromOtherStudents = getMaxCourseNumber(otherStudents);
        Optional<SchoolTombstone> currStudentSchoolTombstone = restUtils.getSchoolByMincode(currStudent.getMincode());
        SchoolTombstone currStudentSchool = currStudentSchoolTombstone.orElseThrow(() ->
                new EntityNotFoundException(SchoolTombstone.class, "SchoolTombstone", currStudent.getMincode()));
        List<Optional<SchoolTombstone>> otherStudentSchoolTombstones = retrieveSchoolTombstones(maxCourseNumberFromOtherStudents, otherStudents);

        if(hasSameNoOfCourses(maxCourseNumberFromOtherStudents, currStudent) &&
                isSameSchoolCategory(otherStudentSchoolTombstones, currStudentSchool) && isSameFacilityType(otherStudentSchoolTombstones, currStudentSchool)) {
            List<Integer> otherSchoolsMincodes = otherStudentSchoolTombstones.stream()
                    .map(sch -> extractRelevantMincode(sch.get()))
                    .toList();

            Integer minOtherSchoolsMincodes = Collections.min(otherSchoolsMincodes);
            Integer currSchoolMincode = extractRelevantMincode(currStudentSchool);
            return currSchoolMincode <= minOtherSchoolsMincodes;
        } else if(currStudent.getNumberOfCourses() != null && Float.parseFloat(currStudent.getNumberOfCourses()) > maxCourseNumberFromOtherStudents){
            return true;
        } else if(hasSameNoOfCourses(maxCourseNumberFromOtherStudents, currStudent) && currStudentSchool.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.PUBLIC.getCode()) && !otherSchoolsIncludePublic(otherStudentSchoolTombstones)) {
            return true;
        }
        else return hasSameNoOfCourses(maxCourseNumberFromOtherStudents, currStudent) && isSameSchoolCategory(otherStudentSchoolTombstones, currStudentSchool)
                    && currStudentSchool.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.STANDARD.getCode()) && !otherSchoolsIncludeStandard(otherStudentSchoolTombstones);
    }

    public boolean otherSchoolsIncludeStandard(List<Optional<SchoolTombstone>> otherStudentSchoolTombstones) {
        return otherStudentSchoolTombstones.stream().flatMap(Optional::stream).anyMatch(tombstone -> FacilityTypeCodes.STANDARD.getCode().equals(tombstone.getFacilityTypeCode()));
    }

    public boolean hasSameNoOfCourses(Float maxCourseNumberFromOtherStudents, UpdateStudentSagaData currStudent) {
        return currStudent.getNumberOfCourses() != null && maxCourseNumberFromOtherStudents ==  Float.parseFloat(currStudent.getNumberOfCourses());
    }

    public boolean isSameSchoolCategory(List<Optional<SchoolTombstone>> otherStudentSchoolTombstones, SchoolTombstone currStudentSchool) {
        if(SchoolCategoryCodes.INDEPENDENTS_AND_OFFSHORE.contains(currStudentSchool.getSchoolCategoryCode())) {
            return otherStudentSchoolTombstones.stream().flatMap(Optional::stream).allMatch(school -> SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode()));
        }
        return otherStudentSchoolTombstones.stream().flatMap(Optional::stream).allMatch(school -> school.getSchoolCategoryCode().equalsIgnoreCase(currStudentSchool.getSchoolCategoryCode()));
    }

    public boolean otherSchoolsIncludePublic(List<Optional<SchoolTombstone>> otherStudentSchoolTombstones) {
        return otherStudentSchoolTombstones.stream().flatMap(Optional::stream).anyMatch(tombstone -> SchoolCategoryCodes.PUBLIC.getCode().equals(tombstone.getSchoolCategoryCode()));
    }

    public boolean isSameFacilityType(List<Optional<SchoolTombstone>> otherStudentSchoolTombstones, SchoolTombstone currStudentSchool) {
        return otherStudentSchoolTombstones.stream().flatMap(Optional::stream).allMatch(school -> school.getFacilityTypeCode().equalsIgnoreCase(currStudentSchool.getFacilityTypeCode()));
    }

    public Float getMaxCourseNumber(List<SdcSchoolCollectionStudentEntity> otherStudents) {
        return otherStudents.stream()
                .map(SdcSchoolCollectionStudentEntity::getNumberOfCourses)
                .filter(Objects::nonNull)
                .map(Float::valueOf)
                .max(Comparator.naturalOrder())
                .orElse(0f);
    }

    public List<Optional<SchoolTombstone>> retrieveSchoolTombstones(Float maxCourseNumberFromOtherStudents, List<SdcSchoolCollectionStudentEntity> otherStudents){
        if (maxCourseNumberFromOtherStudents > 0) {
            return new ArrayList<>(otherStudents.stream().filter(std -> std.getNumberOfCourses() != null && Float.valueOf(std.getNumberOfCourses()).equals(maxCourseNumberFromOtherStudents)).map(std -> restUtils.getSchoolBySchoolID(String.valueOf(std.getSdcSchoolCollection().getSchoolID()))).toList());
        } else {
            return new ArrayList<>(otherStudents.stream().map(std -> restUtils.getSchoolBySchoolID(String.valueOf(std.getSdcSchoolCollection().getSchoolID()))).toList());
        }
    }

    public Integer extractRelevantMincode(SchoolTombstone school){
        if (SchoolCategoryCodes.INDEPENDENTS_AND_OFFSHORE.contains(school.getSchoolCategoryCode())) {
            return Integer.parseInt(school.getMincode());
        } else {
            District district = restUtils.getDistrictByDistrictID(school.getDistrictId()).orElseThrow(() ->
                    new EntityNotFoundException(District.class, "District", school.getDistrictId()));
            return Integer.parseInt(district.getDistrictNumber());
        }
    }

    public void updateSdcStudentEllAndStatus(final Event event, final SdcSagaEntity saga, final UpdateStudentSagaData updateStudentSagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(UPDATE_SDC_STUDENT_STATUS.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        //service call
        if(Objects.equals(updateStudentSagaData.getCollectionTypeCode(), CollectionTypeCodes.SEPTEMBER.getTypeCode())){
            closeCollectionService.updateELLAndMarkStudentAsCompleted(updateStudentSagaData);
        } else {
            closeCollectionService.markStudentAsCompleted(updateStudentSagaData);
        }

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(UPDATE_SDC_STUDENT_STATUS)
                .eventOutcome(SDC_STUDENT_STATUS_UPDATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(updateStudentSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to UPDATE_STUDENT_DOWNSTREAM_SAGA_TOPIC for UPDATE_SDC_STUDENT_STATUS Event.");
    }

    protected void updateGradeYear(final Student studentDataFromEventResponse, final UpdateStudentSagaData updateStudentSagaData) {
        var incomingGradeCode = updateStudentSagaData.getGradeCode();
        if (StringUtils.isBlank(studentDataFromEventResponse.getGradeCode())) {
            studentDataFromEventResponse.setGradeCode(incomingGradeCode);
            val localDateTime = LocalDateTime.now();
            if (localDateTime.getMonthValue() > 6) {
                studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear()));
            } else {
                studentDataFromEventResponse.setGradeYear(String.valueOf(localDateTime.getYear() - 1));
            }
        }
    }

    protected void updateUsualNameFields(final Student studentFromStudentAPI, final UpdateStudentSagaData updateStudentSagaData) {
        studentFromStudentAPI.setUsualFirstName(updateStudentSagaData.getUsualFirstName());
        studentFromStudentAPI.setUsualLastName(updateStudentSagaData.getUsualLastName());
        studentFromStudentAPI.setUsualMiddleNames(updateStudentSagaData.getUsualMiddleNames());
    }
}
