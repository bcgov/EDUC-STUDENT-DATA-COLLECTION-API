package ca.bc.gov.educ.studentdatacollection.api.calculator;

import ca.bc.gov.educ.studentdatacollection.api.calculator.impl.TooYoungCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
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
        SdcSchoolCollectionStudentEntity studentEntity = new SdcSchoolCollectionStudentEntity();
        studentEntity.setIsGraduated(null);
        StudentRuleData studentRuleData = new StudentRuleData();
        studentRuleData.setSdcSchoolCollectionStudentEntity(studentEntity);
        studentRuleData.setSchool(new School());

        doNothing().when(validationRulesService).updatePenMatchAndGradStatusColumns(any(), any());
        when(fteCalculator.calculateFte(any())).thenReturn(new FteCalculationResult());


        // When
        FteCalculationResult result = fteCalculatorChainProcessor.processFteCalculator(studentRuleData);

        // Then
        verify(validationRulesService).updatePenMatchAndGradStatusColumns(studentEntity, null);
    }
}
