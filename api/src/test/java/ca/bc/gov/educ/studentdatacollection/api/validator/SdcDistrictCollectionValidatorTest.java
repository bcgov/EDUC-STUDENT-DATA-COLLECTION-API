package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcDistrictCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class SdcDistrictCollectionValidatorTest {

  @Mock
  private SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  @InjectMocks
  private SdcDistrictCollectionValidator validator;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testValidatePayload_WithCreateOperation_ShouldReturnNoErrors() {
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity();
    sdcMockDistrict.setSdcDistrictCollectionID(null);

    List<FieldError> errors = validator.validatePayload(SdcDistrictCollectionMapper.mapper.toStructure( sdcMockDistrict), true);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidatePayload_WithUpdateOperation_ShouldReturnNoErrors() {
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity();

    when(sdcDistrictCollectionRepository.findById(any())).thenReturn(Optional.of(sdcMockDistrict));

    List<FieldError> errors = validator.validatePayload(SdcDistrictCollectionMapper.mapper.toStructure( sdcMockDistrict), false);
    assertEquals(0, errors.size());
  }

  @Test
  void testValidatePayload_UpdateOperationWithInvalidId_ShouldReturnError() {

    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity();

    when(sdcDistrictCollectionRepository.findById(any())).thenReturn(Optional.empty());

    List<FieldError> errors = validator.validatePayload(SdcDistrictCollectionMapper.mapper.toStructure( sdcMockDistrict), false);
    assertEquals(1, errors.size());
    assertEquals("Invalid SDC district collection ID.", errors.get(0).getDefaultMessage());
  }

  @Test
  void testValidatePayload_WithInvalidStatusCode_ShouldReturnError() {
    SdcDistrictCollectionEntity sdcMockDistrict = createMockSdcDistrictCollectionEntity();
    sdcMockDistrict.setSdcDistrictCollectionID(null);
    sdcMockDistrict.setSdcDistrictCollectionStatusCode("INVALID_STATUS_CODE");

    List<FieldError> errors = validator.validatePayload(SdcDistrictCollectionMapper.mapper.toStructure( sdcMockDistrict), false);
    assertEquals(1, errors.size());
    assertEquals("Invalid SDC district collection status code.", errors.get(0).getDefaultMessage());
  }

  @Test
  void testValidatePayload_WithDBsException_ShouldReturnError() {
    SdcDistrictCollection sdcDistrictCollection = SdcDistrictCollection.builder().sdcDistrictCollectionID(UUID.randomUUID().toString()).build();

    when(sdcDistrictCollectionRepository.findById(any())).thenThrow(new RuntimeException("Database connection failed"));

    List<FieldError> errors = validator.validatePayload(sdcDistrictCollection, false);
    verify(sdcDistrictCollectionRepository, times(1)).findById(any());
    assertEquals(1, errors.size());
    assertEquals("Invalid SDC district collection status code.", errors.get(0).getDefaultMessage());
    verify(sdcDistrictCollectionRepository, times(1)).findById(any());
  }

  public SdcDistrictCollectionEntity createMockSdcDistrictCollectionEntity(){
    SdcDistrictCollectionEntity sdcEntity = new SdcDistrictCollectionEntity();
    sdcEntity.setSdcDistrictCollectionStatusCode("NEW");
    sdcEntity.setSdcDistrictCollectionID(UUID.randomUUID());
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());

    return sdcEntity;
  }
}
