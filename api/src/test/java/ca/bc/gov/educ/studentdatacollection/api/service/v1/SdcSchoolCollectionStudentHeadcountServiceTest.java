package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.EnrollmentHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SdcSchoolCollectionStudentHeadcountServiceTest {

  @Mock
  private SdcSchoolCollectionStudentRepository studentRepository;

  @Mock
  private SdcSchoolCollectionRepository collectionRepository;

  @InjectMocks
  private SdcSchoolCollectionStudentHeadcountService headcountService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void testGetEnrollmentHeadcounts_WithValidCollection() {
    UUID collectionId = UUID.randomUUID();
    SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
    collectionEntity.setSchoolID(UUID.randomUUID());
    when(collectionRepository.findBySdcSchoolCollectionID(collectionId)).thenReturn(Optional.of(collectionEntity));

    EnrollmentHeadcountResult result = mock(EnrollmentHeadcountResult.class);
    when(result.getEnrolledGradeCode()).thenReturn("10");
    when(result.getSchoolAgedHeadcount()).thenReturn(50L);
    when(result.getSchoolAgedEligibleForFte()).thenReturn(48L);
    when(result.getSchoolAgedFteTotal()).thenReturn(new BigDecimal("47.5"));
    when(result.getAdultHeadcount()).thenReturn(13L);
    when(result.getAdultEligibleForFte()).thenReturn(3L);
    when(result.getAdultFteTotal()).thenReturn(new BigDecimal("15"));
    when(result.getTotalHeadcount()).thenReturn(63L);
    when(result.getTotalEligibleForFte()).thenReturn(51L);
    when(result.getTotalFteTotal()).thenReturn(new BigDecimal("62.5"));
    List<EnrollmentHeadcountResult> rawData = Collections.singletonList(result);
    when(studentRepository.getSectionHeadcountsBySchoolId(collectionId)).thenReturn(rawData);


    // Perform the test
    SdcSchoolCollectionStudentHeadcounts headcounts = headcountService.getEnrollmentHeadcounts(collectionId, false);

    // Assertions
    assertEquals("Student Headcount", headcounts.getHeadcountHeaders().get(0).getTitle());
    assertEquals("Grade Headcount", headcounts.getHeadcountHeaders().get(1).getTitle());
    assertNull(headcounts.getHeadcountHeaders().get(0).getColumns().get("All Students").getComparisonValue());
    assertEquals("School Aged", headcounts.getHeadcountTableDataList().get(0).getTitle());
    assertEquals("10", headcounts.getHeadcountTableDataList().get(0).getRows().get(12).getTitle());
    assertEquals("50", headcounts.getHeadcountTableDataList().get(0).getRows().get(12).getColumnTitleAndValueMap().get("Headcount"));
    assertEquals("48", headcounts.getHeadcountTableDataList().get(0).getRows().get(12).getColumnTitleAndValueMap().get("Eligible for FTE"));
    assertEquals("47.5", headcounts.getHeadcountTableDataList().get(0).getRows().get(12).getColumnTitleAndValueMap().get("FTE Total"));
    assertEquals("Total", headcounts.getHeadcountTableDataList().get(0).getRows().get(18).getTitle());
    assertEquals("50", headcounts.getHeadcountTableDataList().get(0).getRows().get(18).getColumnTitleAndValueMap().get("Headcount"));
    assertEquals("48", headcounts.getHeadcountTableDataList().get(0).getRows().get(18).getColumnTitleAndValueMap().get("Eligible for FTE"));
    assertEquals("47.5", headcounts.getHeadcountTableDataList().get(0).getRows().get(18).getColumnTitleAndValueMap().get("FTE Total"));
  }

  @Test
  void testGetEnrollmentHeadcounts_WithInvalidCollection() {
    UUID collectionId = UUID.randomUUID();
    when(collectionRepository.findBySdcSchoolCollectionID(collectionId)).thenReturn(Optional.empty());

    EntityNotFoundException exception = assertThrows(EntityNotFoundException.class,
            () -> headcountService.getEnrollmentHeadcounts(collectionId, false));

    assertTrue(exception.getMessage().contains(collectionId.toString()));
  }

  @Test
  void testGetEnrollmentHeadcounts_WithComparison() {
    UUID collectionId = UUID.randomUUID();
    SdcSchoolCollectionEntity collectionEntity = new SdcSchoolCollectionEntity();
    collectionEntity.setSchoolID(UUID.randomUUID());
    collectionEntity.setCreateDate(LocalDateTime.now());
    when(collectionRepository.findBySdcSchoolCollectionID(collectionId)).thenReturn(Optional.of(collectionEntity));

    EnrollmentHeadcountResult result = mock(EnrollmentHeadcountResult.class);
    when(result.getEnrolledGradeCode()).thenReturn("10");
    when(result.getSchoolAgedHeadcount()).thenReturn(50L);
    when(result.getSchoolAgedEligibleForFte()).thenReturn(48L);
    when(result.getSchoolAgedFteTotal()).thenReturn(new BigDecimal("47.5"));
    when(result.getAdultHeadcount()).thenReturn(13L);
    when(result.getAdultEligibleForFte()).thenReturn(3L);
    when(result.getAdultFteTotal()).thenReturn(new BigDecimal("15"));
    when(result.getTotalHeadcount()).thenReturn(63L);
    when(result.getTotalEligibleForFte()).thenReturn(51L);
    when(result.getTotalFteTotal()).thenReturn(new BigDecimal("62.5"));
    List<EnrollmentHeadcountResult> rawData = Collections.singletonList(result);
    when(studentRepository.getSectionHeadcountsBySchoolId(collectionId)).thenReturn(rawData);

    UUID previousCollectionId = UUID.randomUUID();
    SdcSchoolCollectionEntity previousCollectionEntity = new SdcSchoolCollectionEntity();
    previousCollectionEntity.setSdcSchoolCollectionID(previousCollectionId);
    previousCollectionEntity.setSchoolID(UUID.randomUUID());
    previousCollectionEntity.setCreateDate(LocalDateTime.now());
    when(collectionRepository.findAllBySchoolIDAndCreateDateBetween(any(), any(), any())).thenReturn(List.of(previousCollectionEntity));

    EnrollmentHeadcountResult previousResult = mock(EnrollmentHeadcountResult.class);
    when(previousResult.getEnrolledGradeCode()).thenReturn("10");
    when(previousResult.getSchoolAgedHeadcount()).thenReturn(5L);
    when(previousResult.getSchoolAgedEligibleForFte()).thenReturn(10L);
    when(previousResult.getSchoolAgedFteTotal()).thenReturn(new BigDecimal("25"));
    when(previousResult.getAdultHeadcount()).thenReturn(15L);
    when(previousResult.getAdultEligibleForFte()).thenReturn(20L);
    when(previousResult.getAdultFteTotal()).thenReturn(new BigDecimal("20"));
    when(previousResult.getTotalHeadcount()).thenReturn(20L);
    when(previousResult.getTotalEligibleForFte()).thenReturn(30L);
    when(previousResult.getTotalFteTotal()).thenReturn(new BigDecimal("45"));
    List<EnrollmentHeadcountResult> previousRawData = Collections.singletonList(previousResult);
    when(studentRepository.getSectionHeadcountsBySchoolId(previousCollectionId)).thenReturn(previousRawData);

    // Perform the test
    SdcSchoolCollectionStudentHeadcounts headcounts = headcountService.getEnrollmentHeadcounts(collectionId, true);

    // Assertions
    assertEquals("63", headcounts.getHeadcountHeaders().get(0).getColumns().get("All Students").getCurrentValue());
    assertEquals("20", headcounts.getHeadcountHeaders().get(0).getColumns().get("All Students").getComparisonValue());
  }
}
