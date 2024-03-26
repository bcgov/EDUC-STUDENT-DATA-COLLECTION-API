package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.URL;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import org.junit.jupiter.api.AfterEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SdcDistrictCollectionControllerTest extends BaseStudentDataCollectionAPITest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  SdcSchoolCollectionController sdcSchoolCollectionController;

  @Autowired
  CollectionRepository collectionRepository;

  @Autowired
  SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  @BeforeEach
  public void before() {
  }

  @AfterEach
  public void after() {
    this.collectionRepository.deleteAll();
  }

  @Test
  void testGetAllActiveDistrictCollectionsByDistrictId_GivenNoDistrictsInOpenCollection_ShouldReturnStatusNotFound() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setOpenDate(LocalDateTime.now().minusDays(5));
    collection.setCloseDate(LocalDateTime.now().minusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    var sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print()).andExpect(status().isNotFound());
  }
  @Test
  void testGetAllActiveDistrictCollectionsByDistrictId_withSameDistrictInPastCollection_ShouldReturnOneDistrictCollection() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_READ_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity pastCollection = createMockCollectionEntity();
    pastCollection.setOpenDate(LocalDateTime.now().minusDays(5));
    pastCollection.setCloseDate(LocalDateTime.now().minusDays(2));
    collectionRepository.save(pastCollection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcPastMockDistrict = createMockSdcDistrictCollectionEntity(pastCollection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcPastMockDistrict);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print())
            .andExpect(status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.sdcDistrictCollectionID", equalTo(sdcMockDistrict.getSdcDistrictCollectionID().toString())));
  }

  @Test
  void testGetAllActiveDistrictCollectionsByDistrictId_WithWrongScope_ShouldReturnStatusForbidden() throws Exception {
    final GrantedAuthority grantedAuthority = () -> "SCOPE_SDC_COLLECTION";
    final SecurityMockMvcRequestPostProcessors.OidcLoginRequestPostProcessor mockAuthority = oidcLogin().authorities(grantedAuthority);

    CollectionEntity collection = createMockCollectionEntity();
    collection.setCloseDate(LocalDateTime.now().plusDays(2));
    collectionRepository.save(collection);

    District district = createMockDistrict();
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity(collection, UUID.fromString(district.getDistrictId()));
    sdcDistrictCollectionRepository.save(sdcMockDistrict);

    this.mockMvc.perform(
                    get(URL.BASE_URL_DISTRICT_COLLECTION + "/search/" + district.getDistrictId()).with(mockAuthority))
            .andDo(print()).andExpect(status().isForbidden());
  }
}
