package ca.bc.gov.educ.studentdatacollection.api.collection.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.CollectionController;
import ca.bc.gov.educ.studentdatacollection.api.controller.v1.SdcSchoolCollectionController;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.equalTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcSchoolCollectionControllerTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    SdcSchoolCollectionController sdcSchoolCollectionController;
    @Autowired
    CollectionRepository collectionRepository;
    @Autowired
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @BeforeEach
    public void before() {
    }

    @Test
    void testGetCollectionBySchoolIDAndCollectionID_ShouldReturnSchoolCollection() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_SCHOOL_COLLECTION";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionRepository.save(collection);

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/" + collection.getCollectionID() + "/school/" + school.getSchoolId()).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionID",
                                equalTo(sdcMockSchool.getSdcSchoolCollectionID().toString())));
    }

    @Test
    void testGetCollectionBySchoolIDAndCollectionID_ShouldReturnSchoolCollectionStatus() throws Exception {
        final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_SCHOOL_COLLECTION";
        final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

        CollectionEntity collection = createMockCollectionEntity();
        collection.setCloseDate(LocalDateTime.now().plusDays(2));
        collectionRepository.save(collection);

        School school = createMockSchool();
        SdcSchoolCollectionEntity sdcMockSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()));
        sdcMockSchool.setUploadDate(null);
        sdcMockSchool.setUploadFileName(null);
        sdcSchoolCollectionRepository.save(sdcMockSchool);

        this.mockMvc.perform(
                        get(URL.BASE_URL_SCHOOL_COLLECTION + "/" + collection.getCollectionID() + "/school/" + school.getSchoolId()).with(mockAuthority))
                .andDo(print()).andExpect(status().isOk()).andExpect(
                        MockMvcResultMatchers.jsonPath("$.sdcSchoolCollectionStatusCode",
                                equalTo("NEW")));
    }
}
