package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.reports.AllStudentLightCollectionGenerateCsvService;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Base64;
import java.util.UUID;
import java.util.List;
import java.util.ArrayList;

class AllStudentLightCollectionGenerateCsvServiceTest {

    @Mock
    private SdcSchoolCollectionStudentSearchService mockSearchService;

    @Mock
    private RestUtils mockRestUtils;

    @InjectMocks
    private AllStudentLightCollectionGenerateCsvService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGenerateFromSdcSchoolCollectionID() {
        // Setup
        UUID schoolCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockStudent());

        when(mockSearchService.findAllStudentsLightBySchoolCollectionID(schoolCollectionId)).thenReturn(mockEntities);

        // Execution
        DownloadableReportResponse response = service.generateFromSdcSchoolCollectionID(schoolCollectionId);

        // Verification
        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllStudentsLightBySchoolCollectionID(schoolCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("P.E.N."));
    }

    @Test
    void testGenerateFromSdcDistrictCollectionID() {
        // Setup
        UUID districtCollectionId = UUID.randomUUID();
        List<SdcSchoolCollectionStudentLightEntity> mockEntities = new ArrayList<>();
        mockEntities.add(createMockStudent());  // A method to create mock student data

        when(mockSearchService.findAllStudentsLightByDistrictCollectionId(districtCollectionId)).thenReturn(mockEntities);

        // Execution
        DownloadableReportResponse response = service.generateFromSdcDistrictCollectionID(districtCollectionId);

        // Verification
        assertNotNull(response);
        assertFalse(response.getDocumentData().isEmpty());
        verify(mockSearchService).findAllStudentsLightByDistrictCollectionId(districtCollectionId);
        String decodedData = new String(Base64.getDecoder().decode(response.getDocumentData()));
        assertTrue(decodedData.contains("School ID"));
    }

    private SdcSchoolCollectionStudentLightEntity createMockStudent() {
        SdcSchoolCollectionStudentLightEntity student = new SdcSchoolCollectionStudentLightEntity();
        SdcSchoolCollectionEntity mockCollection = new SdcSchoolCollectionEntity();
        mockCollection.setSchoolID(UUID.randomUUID());
        student.setSdcSchoolCollectionEntity(mockCollection);
        return student;
    }
}
