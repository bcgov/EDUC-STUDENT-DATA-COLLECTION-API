package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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
  private static final String H_CODE_TITLE_KEY = "hHodeKey";
  private static final String OTHER_TITLE_KEY = "otherKey";
  private static final String K_CODE_TITLE_KEY = "kCodeKey";
  private static final String P_CODE_TITLE_KEY = "pCodeKey";
  private static final String Q_CODE_TITLE_KEY = "qCodeKey";
  private static final String R_CODE_TITLE_KEY = "rCodeKey";
  private static final String ALL_LEVELS_TITLE_KEY = "allLevelKey";
  private static final String SCHOOL_NAME = "School Name";
  private final RestUtils restUtils;

  public SpecialEdHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
    this.restUtils = restUtils;
    headcountMethods = getHeadcountMethods();
    sectionTitles = getSelectionTitles();
    rowTitles = getRowTitles();
  }

  public void setGradeCodes(Optional<School> school) {
    if(school.isPresent() && (school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode()) || school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode()))) {
      gradeCodes = SchoolGradeCodes.getIndependentKtoSUGrades();
    } else {
      gradeCodes = SchoolGradeCodes.getNonIndependentKtoSUGrades();
    }
  }

  public void setGradeCodesForDistricts() {
    gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
  }

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, false);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcCollectionID, Boolean isDistrict) {
    SpecialEdHeadcountHeaderResult result = (Boolean.TRUE.equals(isDistrict))
            ? sdcSchoolCollectionStudentRepository.getSpecialEdHeadersByDistrictId(sdcCollectionID)
            : sdcSchoolCollectionStudentRepository.getSpecialEdHeadersBySchoolId(sdcCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    Arrays.asList(A_CODE_TITLE, B_CODE_TITLE, C_CODE_TITLE, D_CODE_TITLE, E_CODE_TITLE, F_CODE_TITLE, G_CODE_TITLE, H_CODE_TITLE, K_CODE_TITLE, P_CODE_TITLE, Q_CODE_TITLE, R_CODE_TITLE).forEach(headerTitle -> {
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

  public HeadcountResultsTable convertHeadcountResultsToSchoolGradeTable(List<SpecialEdHeadcountResult> results) {
    HeadcountResultsTable table = new HeadcountResultsTable();
    List<String> headers = new ArrayList<>();
    headers.add(SCHOOL_NAME);
    Set<String> grades = new HashSet<>();
    Map<String, Map<String, Integer>> schoolGradeCounts = new HashMap<>();
    Map<String, Integer> totalCounts = new HashMap<>();
    Map<String, String> schoolNames = new HashMap<>();

    // Collect all grades and initialize school-grade map
    for (SpecialEdHeadcountResult result : results) {
      grades.add(result.getEnrolledGradeCode());
      schoolGradeCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
      schoolNames.putIfAbsent(result.getSchoolID(), restUtils.getSchoolBySchoolID(result.getSchoolID()).map(School::getDisplayName).orElse("No School Name Found"));
    }

    // Initialize totals for each grade
    for (String grade : gradeCodes) {
      totalCounts.put(grade, 0);
      for (Map<String, Integer> school : schoolGradeCounts.values()) {
        school.putIfAbsent(grade, 0);
      }
    }

    // Sort grades and add to headers
    List<String> sortedGrades = new ArrayList<>(grades);
    Collections.sort(sortedGrades);
    headers.addAll(sortedGrades);
    table.setHeaders(headers);

    // Populate counts for each school and grade
    for (SpecialEdHeadcountResult result : results) {
      Map<String, Integer> gradeCounts = schoolGradeCounts.get(result.getSchoolID());
      String grade = result.getEnrolledGradeCode();
      int count = getCountFromResult(result);
      gradeCounts.merge(grade, count, Integer::sum);
      totalCounts.merge(grade, count, Integer::sum);
    }

    // Create rows for the table, including school names
    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
    schoolGradeCounts.forEach((schoolID, gradesCount) -> {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
      rowData.put(SCHOOL_NAME, HeadcountHeaderColumn.builder().currentValue(schoolNames.get(schoolID)).build());
      gradesCount.forEach((grade, count) -> rowData.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
      rows.add(rowData);
    });

    // Add total row at the end
    Map<String, HeadcountHeaderColumn> totalRow = new LinkedHashMap<>();
    totalRow.put(SCHOOL_NAME, HeadcountHeaderColumn.builder().currentValue("Total").build());
    totalCounts.forEach((grade, count) -> totalRow.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
    rows.add(totalRow);

    table.setRows(rows);
    return table;
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
