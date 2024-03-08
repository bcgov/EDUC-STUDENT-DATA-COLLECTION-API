package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
public class EnrollmentHeadcountHelper extends HeadcountHelper<EnrollmentHeadcountResult> {
  private static final String TOTAL_FTE_TITLE = "FTE Total";
  private static final String ELIGIBLE_FTE_TITLE = "Eligible for FTE";
  private static final String HEADCOUNT_TITLE = "Headcount";
  private static final String UNDER_SCHOOL_AGED_TITLE = "Under School Aged";
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

  public EnrollmentHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
    headcountMethods = getHeadcountMethods();
    sectionTitles = getSelectionTitles();
    rowTitles = getRowTitles();
  }

  public void setGradeCodes(Optional<School> school) {
    if(school.isPresent() && (school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode()) || school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode()))) {
      gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
    } else {
      gradeCodes = SchoolGradeCodes.getNonIndependentSchoolGrades();
    }
  }

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);

    List<EnrollmentHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(previousCollectionRawData);
    List<HeadcountHeader> previousHeadcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(previousCollectionData), getGradesHeadcountTotals(previousCollectionData));
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
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
          .filter(row -> row.get("title").getCurrentValue().equals(HEADCOUNT_TITLE)&& row.get("section").getCurrentValue().equals(ALL_STUDENT_TITLE))
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
          .filter(row -> row.get("title").getCurrentValue().equals(HEADCOUNT_TITLE)&& row.get("section").getCurrentValue().equals(title))
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
}
