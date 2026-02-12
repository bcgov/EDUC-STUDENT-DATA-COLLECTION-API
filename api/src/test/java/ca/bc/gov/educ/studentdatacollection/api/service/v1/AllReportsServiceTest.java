package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.reports.AllReportsService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class AllReportsServiceTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private AllReportsService allReportsService;

    @Autowired
    private CollectionRepository collectionRepository;

    @Autowired
    private SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @Autowired
    private SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @MockBean
    private RestUtils restUtils;


    @Test
    void testGenerateAllDistrictReportsStreamChunked_WithValidDistrictCollection_ShouldStreamNDJSON() throws IOException {
        var district = createMockDistrict();
        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(district));
        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(createMockSchool()));

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collection = collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, null);
        var districtCollectionID = sdcDistrictCollectionRepository.save(sdcDistrictCollection).getSdcDistrictCollectionID();

        SchoolTombstone school = createMockSchool();
        school.setDistrictId(sdcDistrictCollection.getDistrictID().toString());
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setSdcDistrictCollectionID(districtCollectionID);
        sdcSchoolCollectionRepository.save(sdcSchoolCollection);

        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(districtCollectionID, response);

        assertThat(response.getContentType()).isEqualTo("application/x-ndjson;charset=UTF-8");
        assertThat(response.getContentAsString()).isNotEmpty();
        assertThat(response.getContentAsString()).contains("\"type\":\"start\"");
        assertThat(response.getContentAsString()).contains("\"districtNumber\"");
        assertThat(response.getContentAsString()).contains("\"type\":\"complete\"");
    }

    @Test
    void testGenerateAllDistrictReportsStreamChunked_WithInvalidDistrictCollection_ShouldReturn404() throws IOException {
        UUID invalidDistrictCollectionID = UUID.randomUUID();
        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(invalidDistrictCollectionID, response);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testGenerateAllDistrictReportsStreamChunked_WithMissingDistrict_ShouldReturn404() throws IOException {
        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.empty());

        CollectionEntity collection = createMockCollectionEntity();
        collection = collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, null);
        var districtCollectionID = sdcDistrictCollectionRepository.save(sdcDistrictCollection).getSdcDistrictCollectionID();

        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(districtCollectionID, response);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
    }

    @Test
    void testGenerateAllDistrictReportsStreamChunked_WithMultipleSchools_ShouldStreamAllReports() throws IOException {
        var district = createMockDistrict();
        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(district));
        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(createMockSchool()));

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collection = collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, null);
        var districtCollectionID = sdcDistrictCollectionRepository.save(sdcDistrictCollection).getSdcDistrictCollectionID();

        SchoolTombstone school1 = createMockSchool();
        school1.setDistrictId(sdcDistrictCollection.getDistrictID().toString());
        SdcSchoolCollectionEntity sdcSchoolCollection1 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        sdcSchoolCollection1.setSdcDistrictCollectionID(districtCollectionID);

        SchoolTombstone school2 = createMockSchool();
        school2.setDistrictId(sdcDistrictCollection.getDistrictID().toString());
        SdcSchoolCollectionEntity sdcSchoolCollection2 = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        sdcSchoolCollection2.setSdcDistrictCollectionID(districtCollectionID);

        sdcSchoolCollectionRepository.saveAll(java.util.List.of(sdcSchoolCollection1, sdcSchoolCollection2));

        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(districtCollectionID, response);

        String content = response.getContentAsString();
        assertThat(content).contains("\"type\":\"start\"");
        assertThat(content).contains("\"type\":\"progress\"");
        assertThat(content).contains("\"schoolsProcessed\":2");
        assertThat(content).contains("\"type\":\"complete\"");
    }

    @Test
    void testGenerateAllDistrictReportsStreamChunked_ShouldIncludeFileMessages() throws IOException {
        var district = createMockDistrict();
        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(district));
        when(restUtils.getSchoolBySchoolID(any())).thenReturn(Optional.of(createMockSchool()));

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collection = collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, null);
        var districtCollectionID = sdcDistrictCollectionRepository.save(sdcDistrictCollection).getSdcDistrictCollectionID();

        SchoolTombstone school = createMockSchool();
        school.setDistrictId(sdcDistrictCollection.getDistrictID().toString());
        SdcSchoolCollectionEntity sdcSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcSchoolCollection.setSdcDistrictCollectionID(districtCollectionID);
        sdcSchoolCollectionRepository.save(sdcSchoolCollection);

        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(districtCollectionID, response);

        String content = response.getContentAsString();
        assertThat(content).contains("\"type\":\"file\"");
        assertThat(content).contains("\"path\":");
        assertThat(content).contains("\"filename\":");
        assertThat(content).contains("\"data\":");
    }

    @Test
    void testGenerateAllDistrictReportsStreamChunked_ResponseHeaders_ShouldBeSetCorrectly() throws IOException {
        var district = createMockDistrict();
        when(restUtils.getDistrictByDistrictID(any())).thenReturn(Optional.of(district));

        CollectionEntity collection = createMockCollectionEntity();
        collection = collectionRepository.save(collection);

        SdcDistrictCollectionEntity sdcDistrictCollection = createMockSdcDistrictCollectionEntity(collection, null);
        var districtCollectionID = sdcDistrictCollectionRepository.save(sdcDistrictCollection).getSdcDistrictCollectionID();

        MockHttpServletResponse response = new MockHttpServletResponse();

        allReportsService.generateAllDistrictReportsStreamChunked(districtCollectionID, response);

        assertThat(response.getContentType()).isEqualTo("application/x-ndjson;charset=UTF-8");
        assertThat(response.getCharacterEncoding()).isEqualTo("UTF-8");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
        assertThat(response.getHeader("X-Accel-Buffering")).isEqualTo("no");
    }
}



