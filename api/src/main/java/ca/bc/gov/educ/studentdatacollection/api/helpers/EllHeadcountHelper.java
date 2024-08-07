package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class EllHeadcountHelper extends HeadcountHelper<EllHeadcountResult> {

  // Header Titles
  private static final String ELL_TITLE = "English Language Learners";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";

  // Table Row Titles
  private static final String ALL_STUDENTS_TITLE = "English Language Learners";

  // Hash keys
  private static final String TOTAL_ELL_STUDENTS = "totalEllStudents";
  private static final String TOTAL_ELL_ADULT_STUDENTS = "totalAdultEllStudents";
  private static final String TOTAL_ELL_SCHOOL_AGED_STUDENTS = "totalSchoolAgedEllStudents";

  private static final String TOTAL_ELL_ADULT_STUDENTS_TITLE="Adult";
  private static final String TOTAL_ELL_SCHOOL_AGED_STUDENTS_TITLE="School-Aged";
  private static final String ELL_TITLE_KEY="ellLearnerTitleKey";
  public static final String TITLE="title";
  private final RestUtils restUtils;
  public static final String SECTION="section";
  private static final String TOTAL_TITLE = "Total";
  protected Map<String, String> perSchoolReportRowTitles;
  private static final String ALL_ELL_TITLE = "All English Language Learners";

  public EllHeadcountHelper(
          SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
          SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
          SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils
  ) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
    this.restUtils = restUtils;
    headcountMethods = getHeadcountMethods();
    perSchoolReportRowTitles = getPerSchoolReportRowTitles();
    sectionTitles = getSelectionTitles();
    rowTitles = getRowTitles();
  }

  public void setGradeCodes(Optional<SchoolTombstone> school) {
    if(school.isPresent() && SchoolCategoryCodes.INDEPENDENTS.contains(school.get().getSchoolCategoryCode())) {
      gradeCodes = SchoolGradeCodes.getIndependentKtoSUGrades();
    } else {
      gradeCodes = SchoolGradeCodes.getNonIndependentKtoSUGrades();
    }
  }

  public void setGradeCodesForDistricts() {
    gradeCodes = SchoolGradeCodes.getNonIndependentKtoSUGrades();
  }

  public void setComparisonValues(
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity,
    List<HeadcountHeader> headcountHeaderList
  ) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, false);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }

  public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcDistrictCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsByBySchoolIdAndSdcDistrictCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertEllBySchoolHeadcountResults(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawData);
    setResultsTableComparisonValuesDynamic(collectionData, previousCollectionData);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID, boolean isDistrict) {
    EllHeadcountHeaderResult result = isDistrict
            ? sdcSchoolCollectionStudentRepository.getEllHeadersBySdcDistrictCollectionId(sdcSchoolCollectionID)
            : sdcSchoolCollectionStudentRepository.getEllHeadersBySchoolId(sdcSchoolCollectionID);

    List<String> ellColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();

      List.of(ELL_TITLE).forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);

      if (StringUtils.equals(headerTitle, ELL_TITLE)) {
        headcountHeader.setOrderedColumnTitles(ellColumnTitles);
        headcountHeader.getColumns()
          .put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(result.getEligibleStudents())).build());
        headcountHeader.getColumns()
          .put(REPORTED_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(result.getReportedStudents())).build());
      } else { log.warn("Unexpected case headerTitle.  This should not have happened."); }

      headcountHeaderList.add(headcountHeader);
    });
    return headcountHeaderList;
  }

  private Map<String, Function<EllHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<EllHeadcountResult, String>> headcountMethods = new HashMap<>();
    headcountMethods.put(ELL_TITLE_KEY, null);
    headcountMethods.put(TOTAL_ELL_STUDENTS, EllHeadcountResult::getTotalEllStudents);
    headcountMethods.put(TOTAL_ELL_ADULT_STUDENTS, EllHeadcountResult::getTotalAdultEllStudents);
    headcountMethods.put(TOTAL_ELL_SCHOOL_AGED_STUDENTS, EllHeadcountResult::getTotalSchoolAgedEllStudents);
    return headcountMethods;
  }

  private Map<String, String> getSelectionTitles() {
    return Map.of(TOTAL_ELL_STUDENTS, ALL_STUDENTS_TITLE);
  }

  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(TOTAL_ELL_STUDENTS, ALL_STUDENTS_TITLE);
    return rowTitles;
  }

  private Map<String, String> getPerSchoolReportRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(TOTAL_ELL_STUDENTS, TOTAL_ELL_STUDENTS);
    return rowTitles;
  }

  public HeadcountResultsTable convertEllBySchoolHeadcountResults(UUID sdcDistrictCollectionID, List<EllHeadcountResult> results) {
    HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
    List<String> columnTitles = new ArrayList<>(gradeCodes);
    columnTitles.add(0, TITLE);
    columnTitles.add(TOTAL_TITLE);
    headcountResultsTable.setHeaders(columnTitles);
    headcountResultsTable.setRows(new ArrayList<>());

    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

    List<SchoolTombstone> allSchoolsTobmstones = getAllSchoolTombstones(sdcDistrictCollectionID);

    List<SchoolTombstone> schoolResultsTombstones = results.stream()
            .map(value ->  restUtils.getSchoolBySchoolID(value.getSchoolID()).orElseThrow(() ->
                    new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", value.toString())
            )).toList();

    Set<SchoolTombstone> uniqueSchoolTombstones = new HashSet<>(schoolResultsTombstones);
    uniqueSchoolTombstones.addAll(allSchoolsTobmstones);

    Map<String, HeadcountHeaderColumn> titleRow = new LinkedHashMap<>();
    titleRow.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ELL_TITLE).build());
    titleRow.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ELL_TITLE).build());
    rows.add(titleRow);
    uniqueSchoolTombstones.stream().distinct().forEach(school -> createSectionsBySchool(rows, results, school));
    createTotalSection(rows, results);
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }

  public void createSectionsBySchool(List<Map<String, HeadcountHeaderColumn>> rows, List<EllHeadcountResult> results, SchoolTombstone schoolTombstone) {
    for (Map.Entry<String, String> title : perSchoolReportRowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
      rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName()).build());

      BigDecimal total = BigDecimal.ZERO;
      Function<EllHeadcountResult, String> headcountFunction = headcountMethods.get(title.getKey());
      if (headcountFunction != null) {
        for (String gradeCode : gradeCodes) {
          var result = results.stream()
                  .filter(value -> value.getEnrolledGradeCode().equals(gradeCode) && value.getSchoolID().equals(schoolTombstone.getSchoolId()))
                  .findFirst()
                  .orElse(null);
          String headcount = "0";
          if (result != null && result.getEnrolledGradeCode().equals(gradeCode)) {
            headcount = headcountFunction.apply(result);
          }
          rowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(headcount).build());
          total = total.add(new BigDecimal(headcount));
        }
        rowData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(total)).build());
      }
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ELL_TITLE).build());
      rows.add(rowData);
    }
  }

  public void createTotalSection(List<Map<String, HeadcountHeaderColumn>> rows, List<EllHeadcountResult> results) {
    Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
    BigDecimal sectionTotal = BigDecimal.ZERO;

    totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_ELL_TITLE).build());
    totalRowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_ELL_TITLE).build());
    for (String gradeCode : gradeCodes) {
      int totalHeadcountPerGrade = results.stream().filter(grade -> grade.getEnrolledGradeCode().equals(gradeCode))
              .map(EllHeadcountResult::getTotalEllStudents).mapToInt(Integer::valueOf).sum();
      totalRowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(String.valueOf(totalHeadcountPerGrade)).build());
      sectionTotal = sectionTotal.add(new BigDecimal(totalHeadcountPerGrade));
    }
    totalRowData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(sectionTotal)).build());
    rows.add(totalRowData);
  }
}
