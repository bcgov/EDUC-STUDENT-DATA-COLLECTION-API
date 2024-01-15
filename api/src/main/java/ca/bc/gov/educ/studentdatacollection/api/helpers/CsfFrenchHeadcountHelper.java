package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
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
public class CsfFrenchHeadcountHelper extends HeadcountHelper<CsfFrenchHeadcountResult> {
  private static final String FRANCO_TITLE = "Programme Francophone";
  private static final String ADULT_TITLE = "Adult";
  private static final String SCHOOL_AGED_TITLE = "School-Aged";
  private static final String FRANCO_TOTAL_TITLE = "totalFrancophone";
  private static final String FRANCO_SCHOOL_AGE_TITLE = "schoolAgedFrancophone";
  private static final String FRANCO_ADULT_TITLE = "adultFrancophone";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final String NOT_REPORTED_TITLE = "Not Reported";
  private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);

  public CsfFrenchHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
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
  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
    FrenchHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getFrenchHeadersBySchoolId(sdcSchoolCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    HeadcountHeader headcountHeader = new HeadcountHeader();
    headcountHeader.setColumns(new HashMap<>());
    headcountHeader.setTitle(FRANCO_TITLE);
    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalFrancophone()).build());
    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedFrancophone()).build());
    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedFrancophone()))).build());
    headcountHeaderList.add(headcountHeader);
    return headcountHeaderList;
  }

  private Map<String, Function<CsfFrenchHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<CsfFrenchHeadcountResult, String>> headcountMethods = new HashMap<>();
    headcountMethods.put(FRANCO_TOTAL_TITLE, CsfFrenchHeadcountResult::getTotalFrancophone);
    headcountMethods.put(FRANCO_SCHOOL_AGE_TITLE, CsfFrenchHeadcountResult::getSchoolAgedFrancophone);
    headcountMethods.put(FRANCO_ADULT_TITLE, CsfFrenchHeadcountResult::getAdultFrancophone);
    return headcountMethods;
  }

  private Map<String, String> getSelectionTitles() {
    Map<String, String> sectionTitles = new HashMap<>();
    sectionTitles.put(FRANCO_TOTAL_TITLE, FRANCO_TITLE);
    sectionTitles.put(FRANCO_SCHOOL_AGE_TITLE, FRANCO_TITLE);
    sectionTitles.put(FRANCO_ADULT_TITLE, FRANCO_TITLE);
    return sectionTitles;
  }
  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(FRANCO_TOTAL_TITLE, FRANCO_TITLE);
    rowTitles.put(FRANCO_SCHOOL_AGE_TITLE, SCHOOL_AGED_TITLE);
    rowTitles.put(FRANCO_ADULT_TITLE, ADULT_TITLE);
    return rowTitles;
  }
}
