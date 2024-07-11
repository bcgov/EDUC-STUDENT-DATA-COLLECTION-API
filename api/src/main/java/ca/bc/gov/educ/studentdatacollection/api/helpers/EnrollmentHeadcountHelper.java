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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class EnrollmentHeadcountHelper extends HeadcountHelper<EnrollmentHeadcountResult> {
  private static final String TOTAL_FTE_TITLE = "FTE Total";
  private static final String ELIGIBLE_FTE_TITLE = "Eligible for FTE";
  private static final String HEADCOUNT_TITLE = "Headcount";
  private static final String UNDER_SCHOOL_AGED_TITLE = "Preschool Aged";
  private static final String SCHOOL_AGED_TITLE = "School Aged";
  private static final String ADULT_TITLE = "Adult";
  private static final String ALL_STUDENT_TITLE = "All Students";
  private static final String TOTAL_TITLE = "Total";
  private static final String UNDER_SCHOOL_AGED_KEY = "underSchoolAgedTitle";
  private static final String UNDER_SCHOOL_AGED_HEADCOUNT_KEY = "underSchoolAgedHeadcount";
  private static final String UNDER_SCHOOL_AGED_ELIGIBLEKEY = "underSchoolAgedEligible";
  private static final String UNDER_SCHOOL_AGED_FTE_KEY = "underSchoolAgedFte";
  private static final String SCHOOL_AGED_KEY = "schoolAgedTitle";
  private static final String SCHOOL_AGED_HEADCOUNT_KEY = "schoolAgedHeadcount";
  private static final String SCHOOL_AGED_ELIGIBLEKEY = "schoolAgedEligible";
  private static final String SCHOOL_AGED_FTE_KEY = "schoolAgedFte";
  private static final String ADULT_AGED_KEY = "adultTitle";
  private static final String ADULT_AGED_HEADCOUNT_KEY = "adultHeadcount";
  private static final String ADULT_AGED_ELIGIBLEKEY = "adultEligible";
  private static final String ADULT_AGED_FTE_KEY = "adultFte";
  private static final String ALL_AGED_KEY = "allTitle";
  private static final String ALL_AGED_HEADCOUNT_KEY = "allHeadcount";
  private static final String ALL_AGED_ELIGIBLEKEY = "allEligible";
  private static final String ALL_AGED_FTE_KEY = "allFte";
  public static final String SCHOOL_NAME_KEY="schoolName";
  public static final String ALL_SCHOOLS="All Schools";
  public static final String SECTION="section";
  public static final String TITLE="title";
  private final RestUtils restUtils;
  protected Map<String, String> perSchoolRowTitles;
  protected Map<String, String> allSchoolSectionTitles;
  protected Map<String, String> allSchoolRowTitles;

  public EnrollmentHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
      this.restUtils = restUtils;
      headcountMethods = getHeadcountMethods();
      sectionTitles = getSelectionTitles();
      rowTitles = getRowTitles();
      allSchoolSectionTitles = getAllSchoolSectionTitles();
      allSchoolRowTitles = getAllSchoolRowTitles();
      perSchoolRowTitles = getPerSchoolReportRowTitles();
  }

  public void setGradeCodes(Optional<SchoolTombstone> school) {
    if(school.isPresent() && (school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode()) || school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode()))) {
      gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
    } else {
      gradeCodes = SchoolGradeCodes.getNonIndependentSchoolGrades();
    }
  }

  public void setGradeCodesForDistricts() {
    gradeCodes = SchoolGradeCodes.getNonIndependentSchoolGrades();
  }

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);

    List<EnrollmentHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    compareWithPrevCollection(previousCollectionRawData, headcountHeaderList, collectionData);
  }

  public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);

    List<EnrollmentHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcDistrictCollectionId(previousCollectionID);
    compareWithPrevCollection(previousCollectionRawData, headcountHeaderList, collectionData);
  }

  public void compareWithPrevCollection(List<EnrollmentHeadcountResult> previousCollectionRawData, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(previousCollectionRawData);
    List<HeadcountHeader> previousHeadcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(previousCollectionData), getGradesHeadcountTotals(previousCollectionData));
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }

  public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<EnrollmentHeadcountResult> previousCollectionRawDataForHeadcounts = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcDistrictCollectionId(previousCollectionID);
    List<EnrollmentHeadcountResult> prevCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(previousCollectionID);

    HeadcountResultsTable previousCollectionData = convertHeadcountResults(previousCollectionRawDataForHeadcounts);
    HeadcountResultsTable prevCollectionRawDataForTable = convertEnrollmentBySchoolHeadcountResults(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), prevCollectionRawData);
    List<HeadcountHeader> previousHeadcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(previousCollectionData), getGradesHeadcountTotals(previousCollectionData));
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    setResultsTableComparisonValuesDynamic(collectionData, prevCollectionRawDataForTable);
  }


  public HeadcountHeader getGradesHeadcountTotals(HeadcountResultsTable headcountResultsTable) {
    HeadcountHeader headcountTotals = new HeadcountHeader();
    headcountTotals.setTitle("Grade Headcount");
    headcountTotals.setOrderedColumnTitles(gradeCodes);
    headcountTotals.setColumns(new HashMap<>());
    for(String grade : gradeCodes) {
      HeadcountHeaderColumn headcountHeaderColumn = new HeadcountHeaderColumn();
      headcountHeaderColumn.setCurrentValue(String.valueOf(
        headcountResultsTable.getRows().stream()
          .filter(row -> row.get(TITLE).getCurrentValue().equals(HEADCOUNT_TITLE)&& row.get(SECTION).getCurrentValue().equals(ALL_STUDENT_TITLE))
          .mapToLong(row -> Long.parseLong(row.get(grade).getCurrentValue()))
          .sum()));
      headcountTotals.getColumns().put(grade, headcountHeaderColumn);
    }
    return headcountTotals;
  }

  public HeadcountHeader getStudentsHeadcountTotals(HeadcountResultsTable headcountResultsTable) {
    HeadcountHeader studentTotals = new HeadcountHeader();
    studentTotals.setTitle("Student Headcount");
    studentTotals.setOrderedColumnTitles(List.of(UNDER_SCHOOL_AGED_TITLE, SCHOOL_AGED_TITLE, ADULT_TITLE, ALL_STUDENT_TITLE));
    studentTotals.setColumns(new HashMap<>());
    for(String title : List.of(UNDER_SCHOOL_AGED_TITLE, SCHOOL_AGED_TITLE, ADULT_TITLE, ALL_STUDENT_TITLE)) {
      HeadcountHeaderColumn headcountHeaderColumn = new HeadcountHeaderColumn();
      headcountHeaderColumn.setCurrentValue(String.valueOf(
        headcountResultsTable.getRows().stream()
          .filter(row -> row.get(TITLE).getCurrentValue().equals(HEADCOUNT_TITLE)&& row.get(SECTION).getCurrentValue().equals(title))
          .mapToLong(row -> Long.parseLong(row.get(TOTAL_TITLE).getCurrentValue()))
          .sum()));
      studentTotals.getColumns().put(title, headcountHeaderColumn);
    }
    return studentTotals;
  }


  private Map<String, Function<EnrollmentHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<EnrollmentHeadcountResult, String>> headcountMethods = new HashMap<>();
    headcountMethods.put(UNDER_SCHOOL_AGED_KEY, null);
    headcountMethods.put(UNDER_SCHOOL_AGED_HEADCOUNT_KEY, EnrollmentHeadcountResult::getUnderSchoolAgedHeadcount);
    headcountMethods.put(UNDER_SCHOOL_AGED_ELIGIBLEKEY, EnrollmentHeadcountResult::getUnderSchoolAgedEligibleForFte);
    headcountMethods.put(UNDER_SCHOOL_AGED_FTE_KEY, EnrollmentHeadcountResult::getUnderSchoolAgedFteTotal);
    headcountMethods.put(SCHOOL_AGED_KEY, null);
    headcountMethods.put(SCHOOL_AGED_HEADCOUNT_KEY, EnrollmentHeadcountResult::getSchoolAgedHeadcount);
    headcountMethods.put(SCHOOL_AGED_ELIGIBLEKEY, EnrollmentHeadcountResult::getSchoolAgedEligibleForFte);
    headcountMethods.put(SCHOOL_AGED_FTE_KEY, EnrollmentHeadcountResult::getSchoolAgedFteTotal);
    headcountMethods.put(ADULT_AGED_KEY, null);
    headcountMethods.put(ADULT_AGED_HEADCOUNT_KEY, EnrollmentHeadcountResult::getAdultHeadcount);
    headcountMethods.put(ADULT_AGED_ELIGIBLEKEY, EnrollmentHeadcountResult::getAdultEligibleForFte);
    headcountMethods.put(ADULT_AGED_FTE_KEY, EnrollmentHeadcountResult::getAdultFteTotal);
    headcountMethods.put(ALL_AGED_KEY, null);
    headcountMethods.put(ALL_AGED_HEADCOUNT_KEY, EnrollmentHeadcountResult::getTotalHeadcount);
    headcountMethods.put(ALL_AGED_ELIGIBLEKEY, EnrollmentHeadcountResult::getTotalEligibleForFte);
    headcountMethods.put(ALL_AGED_FTE_KEY, EnrollmentHeadcountResult::getTotalFteTotal);
    return headcountMethods;
  }
  private Map<String, String> getSelectionTitles() {
    Map<String, String> sectionTitles = new HashMap<>();
    sectionTitles.put(UNDER_SCHOOL_AGED_KEY, UNDER_SCHOOL_AGED_TITLE);
    sectionTitles.put(UNDER_SCHOOL_AGED_HEADCOUNT_KEY, UNDER_SCHOOL_AGED_TITLE);
    sectionTitles.put(UNDER_SCHOOL_AGED_ELIGIBLEKEY, UNDER_SCHOOL_AGED_TITLE);
    sectionTitles.put(UNDER_SCHOOL_AGED_FTE_KEY, UNDER_SCHOOL_AGED_TITLE);
    sectionTitles.put(SCHOOL_AGED_KEY, SCHOOL_AGED_TITLE);
    sectionTitles.put(SCHOOL_AGED_HEADCOUNT_KEY, SCHOOL_AGED_TITLE);
    sectionTitles.put(SCHOOL_AGED_ELIGIBLEKEY, SCHOOL_AGED_TITLE);
    sectionTitles.put(SCHOOL_AGED_FTE_KEY, SCHOOL_AGED_TITLE);
    sectionTitles.put(ADULT_AGED_KEY, ADULT_TITLE);
    sectionTitles.put(ADULT_AGED_HEADCOUNT_KEY, ADULT_TITLE);
    sectionTitles.put(ADULT_AGED_ELIGIBLEKEY, ADULT_TITLE);
    sectionTitles.put(ADULT_AGED_FTE_KEY, ADULT_TITLE);
    sectionTitles.put(ALL_AGED_KEY, ALL_STUDENT_TITLE);
    sectionTitles.put(ALL_AGED_HEADCOUNT_KEY, ALL_STUDENT_TITLE);
    sectionTitles.put(ALL_AGED_ELIGIBLEKEY, ALL_STUDENT_TITLE);
    sectionTitles.put(ALL_AGED_FTE_KEY, ALL_STUDENT_TITLE);
    return sectionTitles;
  }
  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(UNDER_SCHOOL_AGED_KEY, UNDER_SCHOOL_AGED_TITLE);
    rowTitles.put(UNDER_SCHOOL_AGED_HEADCOUNT_KEY, HEADCOUNT_TITLE);
    rowTitles.put(UNDER_SCHOOL_AGED_ELIGIBLEKEY, ELIGIBLE_FTE_TITLE);
    rowTitles.put(UNDER_SCHOOL_AGED_FTE_KEY, TOTAL_FTE_TITLE);
    rowTitles.put(SCHOOL_AGED_KEY, SCHOOL_AGED_TITLE);
    rowTitles.put(SCHOOL_AGED_HEADCOUNT_KEY, HEADCOUNT_TITLE);
    rowTitles.put(SCHOOL_AGED_ELIGIBLEKEY, ELIGIBLE_FTE_TITLE);
    rowTitles.put(SCHOOL_AGED_FTE_KEY, TOTAL_FTE_TITLE);
    rowTitles.put(ADULT_AGED_KEY, ADULT_TITLE);
    rowTitles.put(ADULT_AGED_HEADCOUNT_KEY, HEADCOUNT_TITLE);
    rowTitles.put(ADULT_AGED_ELIGIBLEKEY, ELIGIBLE_FTE_TITLE);
    rowTitles.put(ADULT_AGED_FTE_KEY, TOTAL_FTE_TITLE);
    rowTitles.put(ALL_AGED_KEY, ALL_STUDENT_TITLE);
    rowTitles.put(ALL_AGED_HEADCOUNT_KEY, HEADCOUNT_TITLE);
    rowTitles.put(ALL_AGED_ELIGIBLEKEY, ELIGIBLE_FTE_TITLE);
    rowTitles.put(ALL_AGED_FTE_KEY, TOTAL_FTE_TITLE);
    return rowTitles;
  }

  private Map<String, String> getAllSchoolRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(ALL_SCHOOLS, null);
    rowTitles.put(HEADCOUNT_TITLE, HEADCOUNT_TITLE);
    rowTitles.put(TOTAL_FTE_TITLE, TOTAL_FTE_TITLE);
    return rowTitles;
  }

  private Map<String, String> getAllSchoolSectionTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(ALL_SCHOOLS, ALL_SCHOOLS);
    rowTitles.put(HEADCOUNT_TITLE, ALL_SCHOOLS);
    rowTitles.put(TOTAL_FTE_TITLE, ALL_SCHOOLS);
    return rowTitles;
  }

  private Map<String, String> getPerSchoolReportRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(SCHOOL_NAME_KEY, null);
    rowTitles.put(ALL_AGED_HEADCOUNT_KEY, HEADCOUNT_TITLE);
    rowTitles.put(ALL_AGED_FTE_KEY, TOTAL_FTE_TITLE);
    return rowTitles;
  }

  public HeadcountResultsTable convertEnrollmentBySchoolHeadcountResults(UUID sdcDistrictCollectionID, List<EnrollmentHeadcountResult> results) {
    HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
    List<String> columnTitles = new ArrayList<>(gradeCodes);
    columnTitles.add(0, TITLE);
    columnTitles.add(TOTAL_TITLE);
    headcountResultsTable.setHeaders(columnTitles);
    headcountResultsTable.setRows(new ArrayList<>());

    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

    List<SdcSchoolCollectionEntity> allSchoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);

    List<SchoolTombstone> allSchoolsTobmstones = allSchoolCollections.stream()
            .map(schoolCollection -> restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString())
                    .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", schoolCollection.getSchoolID().toString())))
            .toList();

    List<SchoolTombstone> schoolResultsTombstones = results.stream()
            .map(value ->  restUtils.getSchoolBySchoolID(value.getSchoolID())
                    .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", value.toString())
            )).toList();

    Set<SchoolTombstone> uniqueSchoolTombstones = new HashSet<>(schoolResultsTombstones);
    uniqueSchoolTombstones.addAll(allSchoolsTobmstones);

    uniqueSchoolTombstones.stream().distinct().forEach(school -> createSectionsBySchool(rows, results, school));
    createAllSchoolSection(rows, results);
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }

  public void createSectionsBySchool(List<Map<String, HeadcountHeaderColumn>> rows, List<EnrollmentHeadcountResult> results, SchoolTombstone schoolTombstone) {
    for (Map.Entry<String, String> title : perSchoolRowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();

      if (title.getKey().equals(SCHOOL_NAME_KEY)) {
        rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName()).build());
      } else {
        rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
      }

      BigDecimal total = BigDecimal.ZERO;
      Function<EnrollmentHeadcountResult, String> headcountFunction = headcountMethods.get(title.getKey());
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
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName()).build());
      rows.add(rowData);
    }
  }

  public void createAllSchoolSection(List<Map<String, HeadcountHeaderColumn>> rows, List<EnrollmentHeadcountResult> results) {
    for (Map.Entry<String, String> row : allSchoolRowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
      BigDecimal sectionTotal = BigDecimal.ZERO;
      if(row.getKey().equals(ALL_SCHOOLS)) {
        totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
      } else {
        totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(row.getValue()).build());
      }
      String section = allSchoolSectionTitles.get(row.getKey());
      if(row.getValue() != null) {
        for (String gradeCode : gradeCodes) {
          if(row.getKey().equals(HEADCOUNT_TITLE)) {
            int totalHeadcountPerGrade = results.stream().filter(grade -> grade.getEnrolledGradeCode().equals(gradeCode))
                    .map(EnrollmentHeadcountResult::getTotalHeadcount).mapToInt(Integer::valueOf).sum();
            totalRowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(String.valueOf(totalHeadcountPerGrade)).build());
            sectionTotal = sectionTotal.add(new BigDecimal(totalHeadcountPerGrade));
          }
          else if(row.getKey().equals(TOTAL_FTE_TITLE)) {
            double totalFtePerGrade = results.stream().filter(grade -> grade.getEnrolledGradeCode().equals(gradeCode))
                    .map(EnrollmentHeadcountResult::getTotalFteTotal).mapToDouble(Double::valueOf).sum();
            totalRowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(String.format("%,.4f", BigDecimal.valueOf(totalFtePerGrade))).build());
            sectionTotal = sectionTotal.add(BigDecimal.valueOf(totalFtePerGrade));
          }
        }
        totalRowData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(String.format("%,.4f", sectionTotal)).build());
      }

      totalRowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(section).build());
      rows.add(totalRowData);
    }
  }

  @Override
  public void setResultsTableComparisonValuesDynamic(HeadcountResultsTable currentCollectionData, HeadcountResultsTable previousCollectionData) {
    Map<String, Map<String, HeadcountHeaderColumn>> previousRowsMap = previousCollectionData.getRows().stream()
            .filter(row -> row.containsKey(SECTION) && row.get(SECTION) != null && row.containsKey(TITLE))
            .collect(Collectors.toMap(
                    row -> row.get(SECTION).getCurrentValue() + "-" + row.get(TITLE).getCurrentValue(),
                    Function.identity(),
                    (existing, replacement) -> existing
            ));

    Map<String, Map<String, HeadcountHeaderColumn>> allTitles = new LinkedHashMap<>();

    currentCollectionData.getRows().forEach(row -> {
      if (row.containsKey(SECTION) && row.get(SECTION) != null && row.containsKey(TITLE)) {
        String key = row.get(SECTION).getCurrentValue() + "-" + row.get(TITLE).getCurrentValue();
        allTitles.put(key, row);
      }
    });

    allTitles.forEach((key, currentRow) -> {
      Map<String, HeadcountHeaderColumn> previousRow = previousRowsMap.getOrDefault(key, new HashMap<>());
      currentRow.forEach((columnKey, currentColumn) -> {
        if (previousRow.containsKey(columnKey)) {
          currentColumn.setComparisonValue(previousRow.get(columnKey).getCurrentValue());
        } else {
          currentColumn.setComparisonValue("0");
        }
      });
    });
  }


}
