package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SpecialEducationHeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
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
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class SpecialEdHeadcountHelper extends HeadcountHelper<SpecialEdHeadcountResult>{
  private static final String LEVEL_1_TITLE = "Level 1";
  private static final String A_CODE_TITLE = "A - Physically Dependent";
  private static final String B_CODE_TITLE = "B - Deafblind";
  private static final String LEVEL_2_TITLE = "Level 2";
  private static final String C_CODE_TITLE = "C - Moderate to Profound Intellectual Disability";
  private static final String D_CODE_TITLE = "D - Physical Disability or Chronic Health Impairment";
  private static final String E_CODE_TITLE = "E - Visual Impairment";
  private static final String F_CODE_TITLE = "F - Deaf or Hard of Hearing";
  private static final String G_CODE_TITLE = "G - Autism Spectrum Disorder";
  private static final String LEVEL_3_TITLE = "Level 3";
  private static final String H_CODE_TITLE = "H - Intensive Behaviour Interventions or Serious Mental Illness";
  private static final String OTHER_TITLE = "Other";
  private static final String K_CODE_TITLE = "K - Mild Intellectual Disability";
  private static final String P_CODE_TITLE = "P - Gifted";
  private static final String Q_CODE_TITLE = "Q - Learning Disability";
  private static final String R_CODE_TITLE = "R - Moderate Behaviour Support/Mental Illness";
  private static final String ALL_LEVELS_TITLE = "All Levels & Categories";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
  private static final String LEVEL_1_TITLE_KEY = "level1Key";
  private static final String A_CODE_TITLE_KEY = "aCodeKey";
  private static final String B_CODE_TITLE_KEY = "bCodeKey";
  private static final String LEVEL_2_TITLE_KEY = "level2Key";
  private static final String C_CODE_TITLE_KEY = "cCodeKey";
  private static final String D_CODE_TITLE_KEY = "dCodeKey";
  private static final String E_CODE_TITLE_KEY = "eCodeKey";
  private static final String F_CODE_TITLE_KEY = "fCodeKey";
  private static final String G_CODE_TITLE_KEY = "gCodeKey";
  private static final String LEVEL_3_TITLE_KEY = "level3Key";
  private static final String H_CODE_TITLE_KEY = "hCodeKey";
  private static final String OTHER_TITLE_KEY = "otherKey";
  private static final String K_CODE_TITLE_KEY = "kCodeKey";
  private static final String P_CODE_TITLE_KEY = "pCodeKey";
  private static final String Q_CODE_TITLE_KEY = "qCodeKey";
  private static final String R_CODE_TITLE_KEY = "rCodeKey";
  private static final String ALL_LEVELS_TITLE_KEY = "allLevelKey";
  private static final String ALL_SCHOOLS = "All Schools";
  private static final String SECTION = "section";
  private static final String TITLE = "title";
  private static final String TOTAL = "Total";
  private final RestUtils restUtils;

  public SpecialEdHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
                                  SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
    this.restUtils = restUtils;
    headcountMethods = getHeadcountMethods();
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

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, false);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousCollectionID(sdcSchoolCollectionEntity);
    List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }

  public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);

    List<SpecialEdHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcDistrictCollectionId(previousCollectionID);
    compareWithPrevCollection(previousCollectionRawData, headcountHeaderList, collectionData, sdcDistrictCollectionEntity);
  }

  public void compareWithPrevCollection(List<SpecialEdHeadcountResult> previousCollectionRawData, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData, SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(previousCollectionRawData);
    UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    setResultsTableComparisonValuesDynamic(collectionData, previousCollectionData);
  }

  public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData, boolean isCat) {
    UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);

    HeadcountResultsTable previousCollectionData;

    if(isCat){
      List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryBySchoolIdAndSdcDistrictCollectionId(previousCollectionID);
      previousCollectionData = convertHeadcountResultsToSpecialEdCategoryTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
    } else {
      List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(previousCollectionID);
      previousCollectionData = convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
    }

    List<HeadcountHeader> previousHeadcountHeaderList = this.getHeaders(previousCollectionID, true);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    setResultsTableComparisonValuesDynamic(collectionData, previousCollectionData);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcCollectionID, Boolean isDistrict) {
    SpecialEdHeadcountHeaderResult result = (Boolean.TRUE.equals(isDistrict))
            ? sdcSchoolCollectionStudentRepository.getSpecialEdHeadersByDistrictId(sdcCollectionID)
            : sdcSchoolCollectionStudentRepository.getSpecialEdHeadersBySchoolId(sdcCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    SpecialEducationHeadcountHeader.getCatTitles().forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);
      headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
      switch (headerTitle) {
        case A_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalEligibleA())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedA())).build());
        }
        case B_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleB()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedB()).build());
        }
        case C_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleC()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedC()).build());
        }
        case D_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalEligibleD())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedD())).build());
        }
        case E_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleE()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedE()).build());
        }
        case F_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleF()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedF()).build());
        }
        case G_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalEligibleG())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedG())).build());
        }
        case H_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleH()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedH()).build());
        }
        case K_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleK()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedK()).build());
        }
        case P_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalEligibleP())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedP())).build());
        }
        case Q_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleQ()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedQ()).build());
        }
        case R_CODE_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEligibleR()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedR()).build());
        }
        default -> {
          log.error("Unexpected header title.  This cannot happen::" + headerTitle);
          throw new StudentDataCollectionAPIRuntimeException("Unexpected header title.  This cannot happen::" + headerTitle);
        }
      }
      headcountHeaderList.add(headcountHeader);
    });
    return headcountHeaderList;
  }

  public HeadcountResultsTable convertHeadcountResultsToSchoolGradeTable(UUID sdcDistrictCollectionID, List<SpecialEdHeadcountResult> results) throws EntityNotFoundException {
    HeadcountResultsTable table = new HeadcountResultsTable();
    List<String> headers = new ArrayList<>();
    Set<String> grades = new HashSet<>(gradeCodes);
    Map<String, Map<String, Integer>> schoolGradeCounts = new HashMap<>();
    Map<String, Integer> totalCounts = new HashMap<>();
    Map<String, String> schoolDetails  = new HashMap<>();

    List<SchoolTombstone> allSchools = getAllSchoolTombstones(sdcDistrictCollectionID);

    // Collect all grades and initialize school-grade map
    for (SpecialEdHeadcountResult result : results) {
      grades.add(result.getEnrolledGradeCode());
      schoolGradeCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
      schoolDetails .putIfAbsent(result.getSchoolID(),
              restUtils.getSchoolBySchoolID(result.getSchoolID())
                      .map(school -> school.getMincode() + " - " + school.getDisplayName())
                      .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", result.getSchoolID())));
    }

    for (SchoolTombstone school : allSchools) {
      schoolGradeCounts.computeIfAbsent(school.getSchoolId(), k -> new HashMap<>());
      schoolDetails.putIfAbsent(school.getSchoolId(), school.getMincode() + " - " + school.getDisplayName());
    }

    // Initialize totals for each grade
    for (String grade : gradeCodes) {
      totalCounts.put(grade, 0);
      schoolGradeCounts.values().forEach(school -> school.putIfAbsent(grade, 0));
    }

    // Sort grades and add to headers
    headers.add(TITLE);
    headers.addAll(gradeCodes);
    headers.add(TOTAL);
    table.setHeaders(headers);

    // Populate counts for each school and grade, and calculate row totals
    Map<String, Integer> schoolTotals = new HashMap<>();
    for (SpecialEdHeadcountResult result : results) {
      if (gradeCodes.contains(result.getEnrolledGradeCode())) {
        Map<String, Integer> gradeCounts = schoolGradeCounts.get(result.getSchoolID());
        String grade = result.getEnrolledGradeCode();
        int count = getCountFromResult(result);
        gradeCounts.merge(grade, count, Integer::sum);
        totalCounts.merge(grade, count, Integer::sum);
        schoolTotals.merge(result.getSchoolID(), count, Integer::sum);
      }
    }

    // Add all schools row at the start
    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
    Map<String, HeadcountHeaderColumn> totalRow = new LinkedHashMap<>();
    totalRow.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
    totalRow.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
    totalCounts.forEach((grade, count) -> totalRow.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
    totalRow.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.values().stream().mapToInt(Integer::intValue).sum())).build());
    rows.add(totalRow);

    // Create rows for the table, including school names
    schoolGradeCounts.forEach((schoolID, gradesCount) -> {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
      rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolDetails.get(schoolID)).build());
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
      gradesCount.forEach((grade, count) -> rowData.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
      rowData.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.getOrDefault(schoolID, 0))).build());
      rows.add(rowData);
    });

    table.setRows(rows);
    return table;
  }

  public HeadcountResultsTable convertHeadcountResultsToSpecialEdCategoryTable(UUID sdcDistrictCollectionID, List<SpecialEdHeadcountResult> results) throws EntityNotFoundException {
    HeadcountResultsTable table = new HeadcountResultsTable();
    List<String> headers = new ArrayList<>();
    List<String> specialEdCategoryTitles = SpecialEducationHeadcountHeader.getCatTitles();
    Map<String, Map<String, Integer>> schoolCategoryCounts = new HashMap<>();
    Map<String, String> schoolDetails  = new HashMap<>();

    List<SchoolTombstone> allSchools = getAllSchoolTombstones(sdcDistrictCollectionID);

    // Collect all special ed categories and initialize school-category map
    for (SpecialEdHeadcountResult result : results) {
      schoolCategoryCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
      schoolDetails.putIfAbsent(result.getSchoolID(),
              restUtils.getSchoolBySchoolID(result.getSchoolID())
                      .map(school -> school.getMincode() + " - " + school.getDisplayName())
                      .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", result.getSchoolID())));
    }

    for (SchoolTombstone school : allSchools) {
      schoolCategoryCounts.computeIfAbsent(school.getSchoolId(), k -> new HashMap<>());
      schoolDetails.putIfAbsent(school.getSchoolId(), school.getMincode() + " - " + school.getDisplayName());
    }

    // Add categories to headers
    headers.add(TITLE);
    headers.addAll(specialEdCategoryTitles);
    headers.add(TOTAL);
    table.setHeaders(headers);

    List<Map<String, HeadcountHeaderColumn>> rows = populateSpedCatRows(results, schoolDetails);

    //Populate category totals
    Map<String, HeadcountHeaderColumn> allSchoolsRow = calcSpedCatTotals(rows);
    rows.add(0, allSchoolsRow);

    table.setRows(rows);
    return table;
  }

  private List<Map<String, HeadcountHeaderColumn>> populateSpedCatRows(List<SpecialEdHeadcountResult> results, Map<String, String> schoolDetails){
    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

    results.forEach(result -> {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
      rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolDetails.get(result.getSchoolID())).build());
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());

      String catACount = result.getAdultsInSpecialEdA() ? result.getSpecialEdACodes() + "*" : result.getSpecialEdACodes();
      rowData.put(SpecialEducationHeadcountHeader.A.getCode(), HeadcountHeaderColumn.builder().currentValue(catACount).build());

      String catBCount = result.getAdultsInSpecialEdB() ? result.getSpecialEdBCodes() + "*" : result.getSpecialEdBCodes();
      rowData.put(SpecialEducationHeadcountHeader.B.getCode(), HeadcountHeaderColumn.builder().currentValue(catBCount).build());

      String catCCount = result.getAdultsInSpecialEdC() ? result.getSpecialEdCCodes() + "*" : result.getSpecialEdCCodes();
      rowData.put(SpecialEducationHeadcountHeader.C.getCode(), HeadcountHeaderColumn.builder().currentValue(catCCount).build());

      String catDCount = result.getAdultsInSpecialEdD() ? result.getSpecialEdDCodes() + "*" : result.getSpecialEdDCodes();
      rowData.put(SpecialEducationHeadcountHeader.D.getCode(), HeadcountHeaderColumn.builder().currentValue(catDCount).build());

      String catECount = result.getAdultsInSpecialEdE() ? result.getSpecialEdDCodes() + "*" : result.getSpecialEdECodes();
      rowData.put(SpecialEducationHeadcountHeader.E.getCode(), HeadcountHeaderColumn.builder().currentValue(catECount).build());

      String catFCount = result.getAdultsInSpecialEdF() ? result.getSpecialEdFCodes() + "*" : result.getSpecialEdFCodes();
      rowData.put(SpecialEducationHeadcountHeader.F.getCode(), HeadcountHeaderColumn.builder().currentValue(catFCount).build());

      String catGCount = result.getAdultsInSpecialEdG() ? result.getSpecialEdGCodes() + "*" : result.getSpecialEdGCodes();
      rowData.put(SpecialEducationHeadcountHeader.G.getCode(), HeadcountHeaderColumn.builder().currentValue(catGCount).build());

      String catHCount = result.getAdultsInSpecialEdH() ? result.getSpecialEdHCodes() + "*" : result.getSpecialEdHCodes();
      rowData.put(SpecialEducationHeadcountHeader.H.getCode(), HeadcountHeaderColumn.builder().currentValue(catHCount).build());

      String catKCount = result.getAdultsInSpecialEdK() ? result.getSpecialEdKCodes() + "*" : result.getSpecialEdKCodes();
      rowData.put(SpecialEducationHeadcountHeader.K.getCode(), HeadcountHeaderColumn.builder().currentValue(catKCount).build());

      String catPCount = result.getAdultsInSpecialEdP() ? result.getSpecialEdPCodes() + "*" : result.getSpecialEdPCodes();
      rowData.put(SpecialEducationHeadcountHeader.P.getCode(), HeadcountHeaderColumn.builder().currentValue(catPCount).build());

      String catQCount = result.getAdultsInSpecialEdQ() ? result.getSpecialEdQCodes() + "*" : result.getSpecialEdQCodes();
      rowData.put(SpecialEducationHeadcountHeader.Q.getCode(), HeadcountHeaderColumn.builder().currentValue(catQCount).build());

      String catRCount = result.getAdultsInSpecialEdR() ? result.getSpecialEdRCodes() + "*" : result.getSpecialEdRCodes();
      rowData.put(SpecialEducationHeadcountHeader.R.getCode(), HeadcountHeaderColumn.builder().currentValue(catRCount).build());

      rowData.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getAllLevels())).build());

      rows.add(rowData);

    });
    return rows;
  }

  private Map<String, HeadcountHeaderColumn>  calcSpedCatTotals(List<Map<String, HeadcountHeaderColumn>> schoolRows){
    Map<String, HeadcountHeaderColumn> allSchoolsRow = new LinkedHashMap<>();
    allSchoolsRow.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
    allSchoolsRow.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());

    int catATotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.A.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.A.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catATotal)).build());

    int catBTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.B.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.B.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catBTotal)).build());

    int catCTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.C.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.C.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catCTotal)).build());

    int catDTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.D.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.D.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catDTotal)).build());

    int catETotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.E.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.E.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catETotal)).build());

    int catFTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.F.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.F.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catFTotal)).build());

    int catGTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.G.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.G.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catGTotal)).build());

    int catHTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.H.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.H.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catHTotal)).build());

    int catKTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.K.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.K.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catKTotal)).build());

    int catPTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.P.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.P.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catPTotal)).build());

  int catQTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.Q.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.Q.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catQTotal)).build());

    int catRTotal = schoolRows.stream().mapToInt(school -> stripAsterisk(school.get(SpecialEducationHeadcountHeader.R.getCode()).getCurrentValue())).sum();
    allSchoolsRow.put(SpecialEducationHeadcountHeader.R.getCode(), HeadcountHeaderColumn.builder().currentValue(String.valueOf(catRTotal)).build());

    int allCatsTotal = catATotal + catBTotal + catCTotal + catDTotal + catETotal + catFTotal + catGTotal + catHTotal + catKTotal + catPTotal + catQTotal + catRTotal;
    allSchoolsRow.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(allCatsTotal)).build());

    return allSchoolsRow;
  }

  private int stripAsterisk(String count){
    String cleanedStr = count.replace("*", "");
    return Integer.parseInt(cleanedStr);
  }

  private int getCountFromResult(SpecialEdHeadcountResult result) {
    try {
      return Integer.parseInt(result.getAllLevels());
    } catch (NumberFormatException e) {
      log.error("Error parsing count from result for SchoolID {}: {}", result.getSchoolID(), e.getMessage());
      return 0;
    }
  }

  private Map<String, Function<SpecialEdHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<SpecialEdHeadcountResult, String>> headcountMethods = new HashMap<>();

    headcountMethods.put(LEVEL_1_TITLE_KEY, SpecialEdHeadcountResult::getLevelOnes);
    headcountMethods.put(A_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdACodes);
    headcountMethods.put(B_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdBCodes);
    headcountMethods.put(LEVEL_2_TITLE_KEY, SpecialEdHeadcountResult::getLevelTwos);
    headcountMethods.put(C_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdCCodes);
    headcountMethods.put(D_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdDCodes);
    headcountMethods.put(E_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdECodes);
    headcountMethods.put(F_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdFCodes);
    headcountMethods.put(G_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdGCodes);
    headcountMethods.put(LEVEL_3_TITLE_KEY, SpecialEdHeadcountResult::getLevelThrees);
    headcountMethods.put(H_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdHCodes);
    headcountMethods.put(OTHER_TITLE_KEY, SpecialEdHeadcountResult::getOtherLevels);
    headcountMethods.put(K_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdKCodes);
    headcountMethods.put(P_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdPCodes);
    headcountMethods.put(Q_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdQCodes);
    headcountMethods.put(R_CODE_TITLE_KEY, SpecialEdHeadcountResult::getSpecialEdRCodes);
    headcountMethods.put(ALL_LEVELS_TITLE_KEY, SpecialEdHeadcountResult::getAllLevels);
    return headcountMethods;
  }
  private Map<String, String> getSelectionTitles() {
    Map<String, String> sectionTitles = new HashMap<>();
    sectionTitles.put(LEVEL_1_TITLE_KEY, LEVEL_1_TITLE);
    sectionTitles.put(A_CODE_TITLE_KEY, LEVEL_1_TITLE);
    sectionTitles.put(B_CODE_TITLE_KEY, LEVEL_1_TITLE);
    sectionTitles.put(LEVEL_2_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(C_CODE_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(D_CODE_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(E_CODE_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(F_CODE_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(G_CODE_TITLE_KEY, LEVEL_2_TITLE);
    sectionTitles.put(LEVEL_3_TITLE_KEY, LEVEL_3_TITLE);
    sectionTitles.put(H_CODE_TITLE_KEY, LEVEL_3_TITLE);
    sectionTitles.put(OTHER_TITLE_KEY, OTHER_TITLE);
    sectionTitles.put(K_CODE_TITLE_KEY, OTHER_TITLE);
    sectionTitles.put(P_CODE_TITLE_KEY, OTHER_TITLE);
    sectionTitles.put(Q_CODE_TITLE_KEY, OTHER_TITLE);
    sectionTitles.put(R_CODE_TITLE_KEY, OTHER_TITLE);
    sectionTitles.put(ALL_LEVELS_TITLE_KEY, ALL_LEVELS_TITLE);
    return sectionTitles;
  }
  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(LEVEL_1_TITLE_KEY, LEVEL_1_TITLE);
    rowTitles.put(A_CODE_TITLE_KEY, A_CODE_TITLE);
    rowTitles.put(B_CODE_TITLE_KEY, B_CODE_TITLE);
    rowTitles.put(LEVEL_2_TITLE_KEY, LEVEL_2_TITLE);
    rowTitles.put(C_CODE_TITLE_KEY, C_CODE_TITLE);
    rowTitles.put(D_CODE_TITLE_KEY, D_CODE_TITLE);
    rowTitles.put(E_CODE_TITLE_KEY, E_CODE_TITLE);
    rowTitles.put(F_CODE_TITLE_KEY, F_CODE_TITLE);
    rowTitles.put(G_CODE_TITLE_KEY, G_CODE_TITLE);
    rowTitles.put(LEVEL_3_TITLE_KEY, LEVEL_3_TITLE);
    rowTitles.put(H_CODE_TITLE_KEY, H_CODE_TITLE);
    rowTitles.put(OTHER_TITLE_KEY, OTHER_TITLE);
    rowTitles.put(K_CODE_TITLE_KEY, K_CODE_TITLE);
    rowTitles.put(P_CODE_TITLE_KEY, P_CODE_TITLE);
    rowTitles.put(Q_CODE_TITLE_KEY, Q_CODE_TITLE);
    rowTitles.put(R_CODE_TITLE_KEY, R_CODE_TITLE);
    rowTitles.put(ALL_LEVELS_TITLE_KEY, ALL_LEVELS_TITLE);
    return rowTitles;
  }
}
