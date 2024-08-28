package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

class HeadcountHelperTest {

  @Mock
  private SdcSchoolCollectionRepository schoolCollectionRepository;

  @InjectMocks
  private HeadcountHelper headcountHelper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }
  @ParameterizedTest
  @CsvSource({
          "title1:10:0,title2:15:20,title3:0:0",
          "title1:0:10,title2:9.8:12,title3:0:-7"
  })
  void testSetComparisonValues(String column1, String column2, String column3) {
    // Given
    String[] column1Array = column1.split(":");
    String[] column2Array = column2.split(":");
    String[] column3Array = column3.split(":");
    Map<String, HeadcountHeaderColumn> currentColumns = new HashMap<>();
    Map<String, HeadcountHeaderColumn> previousColumns = new HashMap<>();
    for (String[] column : Arrays.asList(column1Array, column2Array, column3Array)) {
      currentColumns.put(column[0], new HeadcountHeaderColumn(column[1], null));
      previousColumns.put(column[0], new HeadcountHeaderColumn(column[2], null));
    }

    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    headcountHeaderList.add(HeadcountHeader.builder().columns(currentColumns).orderedColumnTitles(new ArrayList<>(currentColumns.keySet())).build());

    List<HeadcountHeader> previousHeadcountHeaderList = new ArrayList<>();
    previousHeadcountHeaderList.add(HeadcountHeader.builder().columns(previousColumns).orderedColumnTitles(new ArrayList<>(previousColumns.keySet())).build());

    // When
    headcountHelper.setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);

    // Then
    HeadcountHeader modifiedHeader = headcountHeaderList.get(0);
    Map<String, HeadcountHeaderColumn> modifiedColumns = modifiedHeader.getColumns();

    for (String[] column : Arrays.asList(column1Array, column2Array, column3Array)) {
      assertEquals(column[2], modifiedColumns.get(column[0]).getComparisonValue());
      assertEquals(column[1], modifiedColumns.get(column[0]).getCurrentValue());
    }
  }
  @ParameterizedTest
  @CsvSource({
          "2, 15",
          "2, 28",
          "2, 1",
          "7, 15",
          "7, 1",
          "7, 31",
          "9, 15",
          "9, 1",
          "9, 30",
  })
  void testGetPreviousSeptemberCollectionID_givenPreviousCollection_shouldCallRepositoryWithCorrectDates(int month, int day) {
    // Given
    UUID schoolCollectionId = UUID.randomUUID();
    UUID schoolId = UUID.randomUUID();
    LocalDateTime createDate = LocalDateTime.of(2023, month, day, 12, 0);
    SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
    schoolCollectionEntity.setSdcSchoolCollectionID(schoolCollectionId);
    schoolCollectionEntity.setSchoolID(schoolId);
    schoolCollectionEntity.setCreateDate(createDate);

    Optional<SdcSchoolCollectionEntity> septemberCollections = Optional.of(schoolCollectionEntity);

    when(schoolCollectionRepository.findLastCollectionByType(eq(schoolId), any(), any()))
            .thenReturn(septemberCollections);

    // When
    UUID result = headcountHelper.getPreviousSeptemberCollectionID(schoolCollectionEntity);

    // Then
    assertEquals(schoolCollectionId, result);
    verify(schoolCollectionRepository).findLastCollectionByType(
            schoolId,
            CollectionTypeCodes.SEPTEMBER.getTypeCode(),
            schoolCollectionEntity.getSdcSchoolCollectionID());
  }

  @ParameterizedTest
  @CsvSource({
          "2, 15",
          "2, 28",
          "2, 1",
          "7, 15",
          "7, 1",
          "7, 31",
          "9, 15",
          "9, 1",
          "9, 30",
  })
  void testGetPreviousCollectionID_givenPreviousCollection_shouldCallRepositoryWithCorrectDates(int month, int day) {
    // Given
    UUID schoolCollectionId = UUID.randomUUID();
    UUID schoolId = UUID.randomUUID();
    LocalDateTime createDate = LocalDateTime.of(2023, month, day, 12, 0);
    CollectionEntity collectionEntity = new CollectionEntity();
    collectionEntity.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
    SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
    schoolCollectionEntity.setSdcSchoolCollectionID(schoolCollectionId);
    schoolCollectionEntity.setSchoolID(schoolId);
    schoolCollectionEntity.setCreateDate(createDate);
    schoolCollectionEntity.setCollectionEntity(collectionEntity);

    Optional<SdcSchoolCollectionEntity> septemberCollections = Optional.of(schoolCollectionEntity);

    when(schoolCollectionRepository.findLastCollectionByType(eq(schoolId), any(), any()))
            .thenReturn(septemberCollections);

    // When
    UUID result = headcountHelper.getPreviousCollectionID(schoolCollectionEntity, schoolCollectionEntity.getCollectionEntity().getCollectionTypeCode());

    // Then
    assertEquals(schoolCollectionId, result);
    verify(schoolCollectionRepository).findLastCollectionByType(
            schoolId,
            CollectionTypeCodes.SEPTEMBER.getTypeCode(),
            schoolCollectionEntity.getSdcSchoolCollectionID());
  }

  @Test
  void testGetPreviousSeptemberCollectionID_givenNoCollections_shouldReturnNull() {
    // Given
    UUID schoolCollectionId = UUID.randomUUID();
    UUID schoolId = UUID.randomUUID();
    LocalDateTime createDate = LocalDateTime.of(2023, Month.JUNE, 15, 12, 0);
    SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
    schoolCollectionEntity.setSdcSchoolCollectionID(schoolCollectionId);
    schoolCollectionEntity.setSchoolID(schoolId);
    schoolCollectionEntity.setCreateDate(createDate);

    when(schoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(eq(schoolId), any()))
            .thenReturn(new ArrayList<>());

    // When
    UUID result = headcountHelper.getPreviousSeptemberCollectionID(schoolCollectionEntity);

    // Then
    assertNull(result);
  }
  @Test
  void testGetPreviousCollectionID_givenNoCollections_shouldReturnNull() {
    // Given
    UUID schoolCollectionId = UUID.randomUUID();
    UUID schoolId = UUID.randomUUID();
    LocalDateTime createDate = LocalDateTime.of(2023, Month.JUNE, 15, 12, 0);
    CollectionEntity collectionEntity = new CollectionEntity();
    collectionEntity.setCollectionTypeCode(CollectionTypeCodes.SEPTEMBER.getTypeCode());
    SdcSchoolCollectionEntity schoolCollectionEntity = new SdcSchoolCollectionEntity();
    schoolCollectionEntity.setSdcSchoolCollectionID(schoolCollectionId);
    schoolCollectionEntity.setSchoolID(schoolId);
    schoolCollectionEntity.setCreateDate(createDate);
    schoolCollectionEntity.setCollectionEntity(collectionEntity);

    when(schoolCollectionRepository.findAllCollectionsForSchoolInLastTwoYears(eq(schoolId), any()))
            .thenReturn(new ArrayList<>());

    // When
    UUID result = headcountHelper.getPreviousCollectionID(schoolCollectionEntity, schoolCollectionEntity.getCollectionEntity().getCollectionTypeCode());

    // Then
    assertNull(result);
  }
  @Test
  void testStripZeroColumns_givenAtLeastOneValue_shouldReturnColumn() {
    // Given
    Map<String, HeadcountHeaderColumn> columns = new HashMap<>();
    columns.put("col1", HeadcountHeaderColumn.builder().currentValue("10").comparisonValue("0").build());
    columns.put("col2", HeadcountHeaderColumn.builder().currentValue("20").comparisonValue("30").build());
    columns.put("col3", HeadcountHeaderColumn.builder().currentValue("30").comparisonValue("").build());
    columns.put("col4", HeadcountHeaderColumn.builder().currentValue("40").comparisonValue(null).build());
    columns.put("col5", HeadcountHeaderColumn.builder().currentValue("0").comparisonValue("10").build());
    columns.put("col6", HeadcountHeaderColumn.builder().currentValue("").comparisonValue("30").build());
    columns.put("col7", HeadcountHeaderColumn.builder().currentValue(null).comparisonValue("40").build());

    List<String> orderedTitles = new ArrayList<>(columns.keySet());
    HeadcountHeader header = HeadcountHeader.builder().columns(columns).orderedColumnTitles(orderedTitles).build();

    // When
    headcountHelper.stripZeroColumns(header);

    // Then
    assertTrue(header.getColumns().containsKey("col1"));
    assertTrue(header.getColumns().containsKey("col2"));
    assertTrue(header.getColumns().containsKey("col3"));
    assertTrue(header.getColumns().containsKey("col4"));
    assertTrue(header.getColumns().containsKey("col5"));
    assertTrue(header.getColumns().containsKey("col6"));
    assertTrue(header.getColumns().containsKey("col7"));
    assertTrue(header.getOrderedColumnTitles().contains("col1"));
    assertTrue(header.getOrderedColumnTitles().contains("col2"));
    assertTrue(header.getOrderedColumnTitles().contains("col3"));
    assertTrue(header.getOrderedColumnTitles().contains("col4"));
    assertTrue(header.getOrderedColumnTitles().contains("col5"));
    assertTrue(header.getOrderedColumnTitles().contains("col6"));
    assertTrue(header.getOrderedColumnTitles().contains("col7"));
  }
  @Test
  void testStripZeroColumns_givenZeroValues_shouldNotReturnColumn() {
    // Given
    Map<String, HeadcountHeaderColumn> columns = new HashMap<>();
    columns.put("col1", HeadcountHeaderColumn.builder().currentValue(null).comparisonValue("0").build());
    columns.put("col2", HeadcountHeaderColumn.builder().currentValue(null).comparisonValue("").build());
    columns.put("col3", HeadcountHeaderColumn.builder().currentValue(null).comparisonValue(null).build());
    columns.put("col4", HeadcountHeaderColumn.builder().currentValue("0").comparisonValue("").build());
    columns.put("col5", HeadcountHeaderColumn.builder().currentValue("0").comparisonValue("0").build());
    columns.put("col6", HeadcountHeaderColumn.builder().currentValue("0").comparisonValue(null).build());
    columns.put("col7", HeadcountHeaderColumn.builder().currentValue("").comparisonValue("").build());

    List<String> orderedTitles = new ArrayList<>(columns.keySet());
    HeadcountHeader header = HeadcountHeader.builder().columns(columns).orderedColumnTitles(orderedTitles).build();

    // When
    headcountHelper.stripZeroColumns(header);

    // Then
    assertFalse(header.getColumns().containsKey("col1"));
    assertFalse(header.getColumns().containsKey("col2"));
    assertFalse(header.getColumns().containsKey("col3"));
    assertFalse(header.getColumns().containsKey("col4"));
    assertFalse(header.getColumns().containsKey("col5"));
    assertFalse(header.getColumns().containsKey("col6"));
    assertFalse(header.getColumns().containsKey("col7"));
    assertFalse(header.getOrderedColumnTitles().contains("col1"));
    assertFalse(header.getOrderedColumnTitles().contains("col2"));
    assertFalse(header.getOrderedColumnTitles().contains("col3"));
    assertFalse(header.getOrderedColumnTitles().contains("col4"));
    assertFalse(header.getOrderedColumnTitles().contains("col5"));
    assertFalse(header.getOrderedColumnTitles().contains("col6"));
    assertFalse(header.getOrderedColumnTitles().contains("col7"));
  }
}
