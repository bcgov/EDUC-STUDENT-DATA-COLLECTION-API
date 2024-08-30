package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.grad.v1.GradStatusResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;

class ValidationRulesServiceTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private ValidationRulesService validationRulesService;

    @Autowired
    private RestUtils restUtils;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetPenMatchResultMultiPEN() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus("DM");
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        validationRulesService.runAndSetPenMatch(mockStudentEntity, "123456789");

        assertNull(mockStudentEntity.getAssignedStudentId());
        assertSame("MULTI", mockStudentEntity.getPenMatchResult());
    }

    @Test
    void testGetPenMatchResultErrorThrown() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus("D1");
        penMatchResult.setMatchingRecords(new ArrayList<>());
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> validationRulesService.runAndSetPenMatch(mockStudentEntity, "123456789"));
    }

    @Test
    void testGetPenMatchResultErrorThrownNull() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus("D1");
        penMatchResult.setMatchingRecords(null);
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> validationRulesService.runAndSetPenMatch(mockStudentEntity, "123456789"));
    }

    @Test
    void testGetPenMatchResultNewPEN() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus("D0");
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        validationRulesService.runAndSetPenMatch(mockStudentEntity, "123456789");

        assertNull(mockStudentEntity.getAssignedStudentId());
        assertSame("CONFLICT", mockStudentEntity.getPenMatchResult());
    }

    @Test
    void testGetPenMatchResultFoundPEN() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);
        GradStatusResult gradStatusResult = getGradStatusResult();
        when(this.restUtils.getGradStatusResult(any(),any())).thenReturn(gradStatusResult);
        SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setSnapshotDate(LocalDate.now());
        schoolCollectionEntity.setCollectionEntity(collectionEntity);
        mockStudentEntity.setSdcSchoolCollection(schoolCollectionEntity);

        validationRulesService.runAndSetPenMatch(mockStudentEntity, "123456789");

        assertEquals(mockStudentEntity.getAssignedStudentId().toString(), penMatchResult.getMatchingRecords().get(0).getStudentID());
        assertSame("MATCH", mockStudentEntity.getPenMatchResult());
    }

    @Test
    void testGetPenMatchResultFoundPENExceptionOccurred() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);
        GradStatusResult gradStatusResult = getGradStatusResult();
        gradStatusResult.setException("error");
        when(this.restUtils.getGradStatusResult(any(),any())).thenReturn(gradStatusResult);
        SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setSnapshotDate(LocalDate.now());
        schoolCollectionEntity.setCollectionEntity(collectionEntity);
        mockStudentEntity.setSdcSchoolCollection(schoolCollectionEntity);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());

        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> validationRulesService.setGraduationStatus(mockStudentEntity));
    }

    @Test
    void testGetPenMatchResultFoundPENExceptionOccurredParseDate() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);
        GradStatusResult gradStatusResult = getGradStatusResult();
        gradStatusResult.setProgramCompletionDate("10-10-2011");
        when(this.restUtils.getGradStatusResult(any(),any())).thenReturn(gradStatusResult);
        SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setSnapshotDate(LocalDate.now());
        schoolCollectionEntity.setCollectionEntity(collectionEntity);
        mockStudentEntity.setSdcSchoolCollection(schoolCollectionEntity);
        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());

        assertThrows(StudentDataCollectionAPIRuntimeException.class, () -> validationRulesService.setGraduationStatus(mockStudentEntity));
    }

    @Test
    void testGetPenMatchResultFoundPENGradNotFound() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);
        GradStatusResult gradStatusResult = getGradStatusResult();
        gradStatusResult.setException("not found");
        gradStatusResult.setProgramCompletionDate(null);
        when(this.restUtils.getGradStatusResult(any(),any())).thenReturn(gradStatusResult);
        SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
        CollectionEntity collectionEntity = new CollectionEntity();
        collectionEntity.setSnapshotDate(LocalDate.now());
        schoolCollectionEntity.setCollectionEntity(collectionEntity);
        mockStudentEntity.setSdcSchoolCollection(schoolCollectionEntity);

        mockStudentEntity.setAssignedStudentId(UUID.randomUUID());

        validationRulesService.runAndSetPenMatch(mockStudentEntity, UUID.randomUUID().toString());

        validationRulesService.setGraduationStatus(mockStudentEntity);

        assertEquals(mockStudentEntity.getAssignedStudentId().toString(), penMatchResult.getMatchingRecords().get(0).getStudentID());
        assertSame("MATCH", mockStudentEntity.getPenMatchResult());
        assertFalse(mockStudentEntity.getIsGraduated());
    }

    @Test
    void testSetupMergedStudentIdValues_whenHistoricStudentIdsIsNull() {
        // Arrange
        var schoolTombstone = createMockSchoolTombstone();
        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), UUID.fromString(schoolTombstone.getSchoolId())));
        UUID assignedStudentId = UUID.randomUUID();
        mockStudentEntity.setAssignedStudentId(assignedStudentId);
        var studentRuleData = createMockStudentRuleData(mockStudentEntity, schoolTombstone);

        // Mock the behavior of restUtils.getMergedStudentIds
        var mergedStudent = getStudentMergeResult();
        when(this.restUtils.getMergedStudentIds(any(UUID.class), any(UUID.class)))
                .thenReturn(List.of(mergedStudent));

        // Act
        validationRulesService.setupMergedStudentIdValues(studentRuleData);

        // Assert
        assertNotNull(studentRuleData.getHistoricStudentIds());
        assertEquals(2, studentRuleData.getHistoricStudentIds().size());
        assertTrue(studentRuleData.getHistoricStudentIds().contains(UUID.fromString(mergedStudent.getMergeStudentID())));
        assertTrue(studentRuleData.getHistoricStudentIds().contains(assignedStudentId));
    }

    @Test
    void testSetupMergedStudentIdValues_whenNoMergedStudents() {
        // Arrange
        var schoolTombstone = createMockSchoolTombstone();
        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), UUID.fromString(schoolTombstone.getSchoolId())));
        UUID assignedStudentId = UUID.randomUUID();
        mockStudentEntity.setAssignedStudentId(assignedStudentId);
        var studentRuleData = createMockStudentRuleData(mockStudentEntity, schoolTombstone);

        when(this.restUtils.getMergedStudentIds(any(UUID.class), any(UUID.class)))
                .thenReturn(List.of());

        // Act
        validationRulesService.setupMergedStudentIdValues(studentRuleData);

        // Assert
        assertNotNull(studentRuleData.getHistoricStudentIds());
        assertEquals(1, studentRuleData.getHistoricStudentIds().size());
        assertTrue(studentRuleData.getHistoricStudentIds().contains(assignedStudentId));
    }

    @Test
    void testSetupMergedStudentIdValues_whenHistoricStudentIdsIsNotNull() {
        // Arrange
        var schoolTombstone = createMockSchoolTombstone();
        SdcSchoolCollectionStudentEntity mockStudentEntity = createMockSchoolStudentEntity(createMockSdcSchoolCollectionEntity(createMockCollectionEntity(), UUID.fromString(schoolTombstone.getSchoolId())));
        UUID assignedStudentId = UUID.randomUUID();
        mockStudentEntity.setAssignedStudentId(assignedStudentId);
        var studentRuleData = createMockStudentRuleData(mockStudentEntity, schoolTombstone);
        studentRuleData.setHistoricStudentIds(List.of(assignedStudentId));

        // Act
        validationRulesService.setupMergedStudentIdValues(studentRuleData);

        // Assert
        assertEquals(1, studentRuleData.getHistoricStudentIds().size());
        assertTrue(studentRuleData.getHistoricStudentIds().contains(assignedStudentId));
    }
}
