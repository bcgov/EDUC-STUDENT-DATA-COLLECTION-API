package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllHeadcountHeaderResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class EllHeadcountHelper extends HeadcountHelper<EllHeadcountResult> {

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
  private static final String ONE_TO_FIVE_TITLE = "1-5 Years";
  private static final String SIX_PLUS_TITLE = "6+ Years";

  // Hash keys
  private static final String SCHOOL_AGED_1_5 = "schoolAgedOneThroughFive";
  private static final String SCHOOL_AGED_6_PLUS = "schoolAgedSixPlus";
  private static final String SCHOOL_AGED_TOTALS = "schoolAgedTotals";
  private static final String ADULT_1_5 = "adultOneThroughFive";
  private static final String ADULT_6_PLUS = "adultSixPlus";
  private static final String ADULT_TOTALS = "adultTotals";
  private static final String ALL_1_5 = "allOneThroughFive";
  private static final String ALL_6_PLUS= "allSixPlus";
  private static final String TOTAL_ELL_STUDENTS = "totalEllStudents";

  public EllHeadcountHelper(
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository
  ) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
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
    List<String> ellColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
    List<String> yearsInEllHeadcountTitles = List.of(ONE_TO_FIVE_TITLE, SIX_PLUS_TITLE);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();

    Arrays.asList(ELL_TITLE, YEARS_IN_ELL_TITLE).forEach(headerTitle -> {
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
        headcountHeader.getColumns()
          .put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(
                  Long.parseLong(result.getAllStudents())
                  - Long.parseLong(result.getReportedStudents()))).build());
      } else if (StringUtils.equals(headerTitle, YEARS_IN_ELL_TITLE)) {
        headcountHeader.setOrderedColumnTitles(yearsInEllHeadcountTitles);
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

  private Map<String, Function<EllHeadcountResult, String>> getHeadcountMethods() {
    return Map.of(
        TOTAL_ELL_STUDENTS, EllHeadcountResult::getTotalEllStudents);
  }

  private Map<String, String> getSelectionTitles() {
    return Map.of(TOTAL_ELL_STUDENTS, ALL_STUDENTS_TITLE);
  }

  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(TOTAL_ELL_STUDENTS, ALL_STUDENTS_TITLE);
    return rowTitles;
  }
}
