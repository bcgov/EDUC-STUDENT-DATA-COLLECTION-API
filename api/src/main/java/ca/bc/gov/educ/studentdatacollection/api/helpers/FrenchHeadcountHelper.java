package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchHeadcountHeaderResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class FrenchHeadcountHelper extends HeadcountHelper<FrenchHeadcountResult> {
  private static final String CORE_FRENCH_TITLE = "Core French";
  private static final String EARLY_FRENCH_TITLE = "Early French Immersion";
  private static final String LATE_FRENCH_TITLE = "Late French Immersion";
  private static final String FRANCO_TITLE = "Programme Francophone";
  private static final String TOTAL_FRENCH_TITLE = "All French Programs";
  private static final String ADULT_TITLE = "Adult";
  private static final String SCHOOL_AGED_TITLE = "School-Aged";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final String NOT_REPORTED_TITLE = "Not Reported";
  private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
  private static final String CORE_TOTAL_TITLE = "totalCoreFrench";
  private static final String CORE_SCHOOL_AGE_TITLE = "schoolAgedCoreFrench";
  private static final String CORE_ADULT_TITLE = "adultCoreFrench";
  private static final String EARLY_TOTAL_TITLE = "totalEarlyFrench";
  private static final String EARLY_SCHOOL_AGE_TITLE = "schoolAgedEarlyFrench";
  private static final String EARLY_ADULT_TITLE = "adultEarlyFrench";
  private static final String LATE_TOTAL_TITLE = "totalLateFrench";
  private static final String LATE_SCHOOL_AGE_TITLE = "schoolAgedLateFrench";
  private static final String LATE_ADULT_TITLE = "adultLateFrench";
  private static final String FRANCO_TOTAL_TITLE = "totalFrancophone";
  private static final String FRANCO_SCHOOL_AGE_TITLE = "schoolAgedFrancophone";
  private static final String FRANCO_ADULT_TITLE = "adultFrancophone";
  private static final String ALL_TOTAL_TITLE = "allTotal";
  private static final String ALL_SCHOOL_AGE_TITLE = "allSchoolAged";
  private static final String ALL_ADULT_TITLE = "allAdult";

  public FrenchHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
    headcountMethods = getHeadcountMethods();
    sectionTitles = getSelectionTitles();
    rowTitles = getRowTitles();
    gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
  }

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
    FrenchHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getFrenchHeadersBySchoolId(sdcSchoolCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    Arrays.asList(CORE_FRENCH_TITLE, EARLY_FRENCH_TITLE, LATE_FRENCH_TITLE, FRANCO_TITLE).forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);
      headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
      switch (headerTitle) {
        case CORE_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalCoreFrench())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedCoreFrench())).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedCoreFrench()))).build());
        }
        case EARLY_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEarlyFrench()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedEarlyFrench()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedEarlyFrench()))).build());
        }
        case LATE_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalLateFrench()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedLateFrench()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedLateFrench()))).build());
        }
        case FRANCO_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalFrancophone()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedFrancophone()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedFrancophone()))).build());
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

  private Map<String, Function<FrenchHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<FrenchHeadcountResult, String>> headcountMethods = new HashMap<>();

    headcountMethods.put(CORE_TOTAL_TITLE, FrenchHeadcountResult::getTotalCoreFrench);
    headcountMethods.put(CORE_SCHOOL_AGE_TITLE, FrenchHeadcountResult::getSchoolAgedCoreFrench);
    headcountMethods.put(CORE_ADULT_TITLE, FrenchHeadcountResult::getAdultCoreFrench);
    headcountMethods.put(EARLY_TOTAL_TITLE, FrenchHeadcountResult::getTotalEarlyFrench);
    headcountMethods.put(EARLY_SCHOOL_AGE_TITLE, FrenchHeadcountResult::getSchoolAgedEarlyFrench);
    headcountMethods.put(EARLY_ADULT_TITLE, FrenchHeadcountResult::getAdultEarlyFrench);
    headcountMethods.put(LATE_TOTAL_TITLE, FrenchHeadcountResult::getTotalLateFrench);
    headcountMethods.put(LATE_SCHOOL_AGE_TITLE, FrenchHeadcountResult::getSchoolAgedLateFrench);
    headcountMethods.put(LATE_ADULT_TITLE, FrenchHeadcountResult::getAdultLateFrench);
    headcountMethods.put(FRANCO_TOTAL_TITLE, FrenchHeadcountResult::getTotalFrancophone);
    headcountMethods.put(FRANCO_SCHOOL_AGE_TITLE, FrenchHeadcountResult::getSchoolAgedFrancophone);
    headcountMethods.put(FRANCO_ADULT_TITLE, FrenchHeadcountResult::getAdultFrancophone);
    headcountMethods.put(ALL_TOTAL_TITLE, FrenchHeadcountResult::getTotalTotals);
    headcountMethods.put(ALL_SCHOOL_AGE_TITLE, FrenchHeadcountResult::getSchoolAgedTotals);
    headcountMethods.put(ALL_ADULT_TITLE, FrenchHeadcountResult::getAdultTotals);
    return headcountMethods;
  }
  private Map<String, String> getSelectionTitles() {
    Map<String, String> sectionTitles = new HashMap<>();
    sectionTitles.put(CORE_TOTAL_TITLE, CORE_FRENCH_TITLE);
    sectionTitles.put(CORE_SCHOOL_AGE_TITLE, CORE_FRENCH_TITLE);
    sectionTitles.put(CORE_ADULT_TITLE, CORE_FRENCH_TITLE);
    sectionTitles.put(EARLY_TOTAL_TITLE, EARLY_FRENCH_TITLE);
    sectionTitles.put(EARLY_SCHOOL_AGE_TITLE, EARLY_FRENCH_TITLE);
    sectionTitles.put(EARLY_ADULT_TITLE, EARLY_FRENCH_TITLE);
    sectionTitles.put(LATE_TOTAL_TITLE, LATE_FRENCH_TITLE);
    sectionTitles.put(LATE_SCHOOL_AGE_TITLE, LATE_FRENCH_TITLE);
    sectionTitles.put(LATE_ADULT_TITLE, LATE_FRENCH_TITLE);
    sectionTitles.put(FRANCO_TOTAL_TITLE, FRANCO_TITLE);
    sectionTitles.put(FRANCO_SCHOOL_AGE_TITLE, FRANCO_TITLE);
    sectionTitles.put(FRANCO_ADULT_TITLE, FRANCO_TITLE);
    sectionTitles.put(ALL_TOTAL_TITLE, TOTAL_FRENCH_TITLE);
    sectionTitles.put(ALL_SCHOOL_AGE_TITLE, TOTAL_FRENCH_TITLE);
    sectionTitles.put(ALL_ADULT_TITLE, TOTAL_FRENCH_TITLE);
    return sectionTitles;
  }
  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(CORE_TOTAL_TITLE, CORE_FRENCH_TITLE);
    rowTitles.put(CORE_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(CORE_ADULT_TITLE, ADULT_TITLE);
    rowTitles.put(EARLY_TOTAL_TITLE, EARLY_FRENCH_TITLE);
    rowTitles.put(EARLY_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(EARLY_ADULT_TITLE, ADULT_TITLE);
    rowTitles.put(LATE_TOTAL_TITLE, LATE_FRENCH_TITLE);
    rowTitles.put(LATE_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(LATE_ADULT_TITLE, ADULT_TITLE);
    rowTitles.put(FRANCO_TOTAL_TITLE, FRANCO_TITLE);
    rowTitles.put(FRANCO_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(FRANCO_ADULT_TITLE, ADULT_TITLE);
    rowTitles.put(ALL_TOTAL_TITLE, TOTAL_FRENCH_TITLE);
    rowTitles.put(ALL_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(ALL_ADULT_TITLE, ADULT_TITLE);
    return rowTitles;
  }
}
