package ca.bc.gov.educ.studentdatacollection.api.calculator.impl;

import ca.bc.gov.educ.studentdatacollection.api.calculator.FteCalculator;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.StudentRuleData;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.FteCalculationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PrpOrYouthSchoolCalculatorTest {

    private PRPorYouthSchoolCalculator prPorYouthSchoolCalculator;
    private FteCalculator nextCalculator;

    @BeforeEach
    public void setup() {
        nextCalculator = mock(FteCalculator.class);
        prPorYouthSchoolCalculator = new PRPorYouthSchoolCalculator();
        prPorYouthSchoolCalculator.setNext(nextCalculator);
    }

    @Test
    void testCalculateFte_StudentPrpSchool() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        student.setDob(format.format(LocalDateTime.now().minusYears(6)));
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        studentData.setSchool(createSchool());

        // When
        FteCalculationResult result = prPorYouthSchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals(ZeroFteReasonCodes.PRP_OR_YOUTH_SCHOOL.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentYouthSchool() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        student.setDob(format.format(LocalDateTime.now().minusYears(6)));
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        var school = createSchool();
        school.setFacilityTypeCode(FacilityTypeCodes.YOUTH.getCode());
        studentData.setSchool(school);

        // When
        FteCalculationResult result = prPorYouthSchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(BigDecimal.ZERO, result.getFte());
        assertEquals(ZeroFteReasonCodes.PRP_OR_YOUTH_SCHOOL.getCode(), result.getFteZeroReason());
        verify(nextCalculator, never()).calculateFte(any());
    }

    @Test
    void testCalculateFte_StudentExactlyOldEnough() {
        // Given
        SdcSchoolCollectionStudentEntity student = new SdcSchoolCollectionStudentEntity();
        DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyyMMdd");
        student.setDob(format.format(LocalDateTime.now().minusYears(6)));
        StudentRuleData studentData = new StudentRuleData();
        studentData.setSdcSchoolCollectionStudentEntity(student);
        var school = createSchool();
        school.setFacilityTypeCode(FacilityTypeCodes.STANDARD.getCode());
        studentData.setSchool(school);

        // When
        FteCalculationResult expectedResult = new FteCalculationResult();
        expectedResult.setFte(BigDecimal.ONE);
        expectedResult.setFteZeroReason(null);

        when(nextCalculator.calculateFte(any())).thenReturn(expectedResult);
        FteCalculationResult result = prPorYouthSchoolCalculator.calculateFte(studentData);

        // Then
        assertEquals(expectedResult, result);
        verify(nextCalculator).calculateFte(studentData);
    }

    private SchoolTombstone createSchool(){
        SchoolTombstone schoolTombstone = new SchoolTombstone();
        schoolTombstone.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
        schoolTombstone.setFacilityTypeCode(FacilityTypeCodes.LONG_PRP.getCode());
        schoolTombstone.setDistrictId(UUID.randomUUID().toString());
        return schoolTombstone;
    }
}
