package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;

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
    void testGetPenMatchResultNewPEN() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        penMatchResult.setPenStatus("DM");
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        validationRulesService.updatePenMatchAndGradStatusColumns(mockStudentEntity, "123456789");

        assertNull(mockStudentEntity.getAssignedStudentId());
        assertSame("NEW", mockStudentEntity.getPenMatchResult());
    }

    @Test
    void testGetPenMatchResultFoundPEN() {
        SdcSchoolCollectionStudentEntity mockStudentEntity = new SdcSchoolCollectionStudentEntity();
        PenMatchResult penMatchResult = getPenMatchResult();
        when(this.restUtils.getPenMatchResult(any(),any(), anyString())).thenReturn(penMatchResult);

        validationRulesService.updatePenMatchAndGradStatusColumns(mockStudentEntity, "123456789");

        assertEquals(mockStudentEntity.getAssignedStudentId().toString(), penMatchResult.getMatchingRecords().get(0).getStudentID());
        assertSame(mockStudentEntity.getPenMatchResult(), penMatchResult.getPenStatus());
    }
}
