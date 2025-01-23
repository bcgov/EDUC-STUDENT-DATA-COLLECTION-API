package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class FteCalculatorChainProcessorTest {
    @Mock
    private ValidationRulesService validationRulesService;
    private FteCalculator fteCalculator;
    private FteCalculatorChainProcessor fteCalculatorChainProcessor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        fteCalculator = mock(FteCalculator.class);
        List<FteCalculator> fteCalculators = new ArrayList<>();
        fteCalculators.add(fteCalculator);
        fteCalculatorChainProcessor = new FteCalculatorChainProcessor(fteCalculators, validationRulesService);
    }

    @Test
    void testProcessFteCalculator_WhenIsGraduatedIsNull_ThenGradStatusCalculated() {
        // Given
        CollectionEntity collection = new CollectionEntity();
        collection.setCollectionID(UUID.randomUUID());
        collection.setSnapshotDate(LocalDate.now());
        SdcSchoolCollectionEntity sdcSchoolCollection = new SdcSchoolCollectionEntity();
        sdcSchoolCollection.setCollectionEntity(collection);
        SdcSchoolCollectionStudentEntity studentEntity = new SdcSchoolCollectionStudentEntity();
        studentEntity.setSdcSchoolCollection(sdcSchoolCollection);
        studentEntity.setIsGraduated(null);
        StudentRuleData studentRuleData = new StudentRuleData();
        studentRuleData.setSdcSchoolCollectionStudentEntity(studentEntity);
        studentRuleData.setSchool(new SchoolTombstone());

        doNothing().when(validationRulesService).runAndSetPenMatch(any(), any());
        when(fteCalculator.calculateFte(any())).thenReturn(new FteCalculationResult());


        // When
        fteCalculatorChainProcessor.processFteCalculator(studentRuleData);

        // Then
        verify(validationRulesService).setupPENMatchAndEllAndGraduateValues(studentRuleData);
    }
}
