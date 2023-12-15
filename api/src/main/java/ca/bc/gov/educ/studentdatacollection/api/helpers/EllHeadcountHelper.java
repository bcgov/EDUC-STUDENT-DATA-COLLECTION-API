package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class EllHeadcountHelper extends HeadcountHelper {

  // Header Titles
  private static final String ELL_TITLE = "English Language Learners";
  private static final String YEARS_IN_ELL_TITLE = "Years in ELL Headcount";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final String NOT_REPORTED_TITLE = "Not Reported";

  // Table Row Titles
  private static final String SCHOOL_AGED_TITLE = "School Aged English Language Learners";
  private static final String ADULT_TITLE = "Adult English Language Learners";
  private static final String ALL_STUDENTS_TITLE = "All English Language Learners";

  // Sub-row titles (used in header and table)
  private static final String ONE_TO_FIVE_TITLE = "Year 1-5";
  private static final String SIX_PLUS_TITLE = "Year 6+";

  // Total column title in table data
  private static final String TOTAL_GRADE_TITLE = "Total";

  public EllHeadcountHelper(
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository
  ) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
  }

  public void setComparisonValues(
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity,
    List<HeadcountHeader> headcountHeaderList
  ) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
    EllHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository
      .getEllHeadersBySchoolId(sdcSchoolCollectionID);
    List<String> headerColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();

    Arrays.asList(ELL_TITLE, YEARS_IN_ELL_TITLE).forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);
      headcountHeader.setOrderedColumnTitles(headerColumnTitles);

      if (StringUtils.equals(headerTitle, ELL_TITLE)) {
        headcountHeader.getColumns()
          .put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(result.getEligibleStudents())).build());
        headcountHeader.getColumns()
          .put(REPORTED_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(result.getReportedStudents())).build());
        headcountHeader.getColumns()
          .put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(
                  Long.parseLong(result.getAllStudents())
                  - Long.parseLong(result.getReportedStudents()))).build());
      } else if (StringUtils.equals(headerTitle, YEARS_IN_ELL_TITLE)) {
        headcountHeader.getColumns()
          .put(ONE_TO_FIVE_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(result.getOneToFiveYears()).build());
        headcountHeader.getColumns()
          .put(SIX_PLUS_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(result.getSixPlusYears()).build());
      } else { log.warn("Unexpected case headerTitle.  This should not have happened."); }

      headcountHeaderList.add(headcountHeader);
    });
    return headcountHeaderList;
  }

  public List<HeadcountTableData> convertHeadcountResults(List<EllHeadcountResult> results) {
    List<HeadcountTableData> headcountTableDataList = new ArrayList<>();
    List<String> gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
    String[] titles = { SCHOOL_AGED_TITLE, ADULT_TITLE, ALL_STUDENTS_TITLE };

    for (String title : titles) {
      List<HeadCountTableDataRow> rows = new ArrayList<>();
      for (String gradeCode : gradeCodes) {
        EllHeadcountResult result = results.stream()
          .filter(value -> value.getEnrolledGradeCode().equals(gradeCode))
          .findFirst()
          .orElse(null);
        rows.add(buildDataRow(result, title, gradeCode));
      }

      String[] keys = { title, ONE_TO_FIVE_TITLE, SIX_PLUS_TITLE };
      rows.add(calculateSummaryRow(rows, keys, TOTAL_GRADE_TITLE));
      headcountTableDataList.add(buildHeadcountTableData(title, rows, List.of(keys)));
    }
    return headcountTableDataList;
  }

  public HeadCountTableDataRow buildDataRow(EllHeadcountResult result, String title, String gradeCode) {
    Map<String, String> valuesMap = new HashMap<>();

    if (result != null) {
      switch (title) {
        case SCHOOL_AGED_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedTotals()));
          valuesMap.put(ONE_TO_FIVE_TITLE, String.valueOf(result.getSchoolAgedOneThroughFive()));
          valuesMap.put(SIX_PLUS_TITLE, String.valueOf(result.getSchoolAgedSixPlus()));
        }
        case ADULT_TITLE -> {
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultTotals()));
          valuesMap.put(ONE_TO_FIVE_TITLE, String.valueOf(result.getAdultOneThroughFive()));
          valuesMap.put(SIX_PLUS_TITLE, String.valueOf(result.getAdultSixPlus()));
        }
        case ALL_STUDENTS_TITLE -> {
          valuesMap.put(ALL_STUDENTS_TITLE, String.valueOf(result.getTotalEllStudents()));
          valuesMap.put(ONE_TO_FIVE_TITLE, String.valueOf(result.getAllOneThroughFive()));
          valuesMap.put(SIX_PLUS_TITLE, String.valueOf(result.getAllSixPlus()));
        }
        default -> log.warn("Unexpected case in buildDataRow. This should not have happened.");
      }
    } else {
      valuesMap.put(title, "0");
      valuesMap.put(ONE_TO_FIVE_TITLE, "0");
      valuesMap.put(SIX_PLUS_TITLE, "0");
    }

    return HeadCountTableDataRow.builder()
      .title(gradeCode)
      .columnTitleAndValueMap(valuesMap)
      .build();
  }
}
