package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final String NOT_REPORTED_TITLE = "Not Reported";

  // Table Row Titles
  private static final String ALL_STUDENTS_TITLE = "English Language Learners";

  // Hash keys
  private static final String TOTAL_ELL_STUDENTS = "totalEllStudents";

  public EllHeadcountHelper(
    SdcSchoolCollectionRepository sdcSchoolCollectionRepository,
    SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository
  ) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
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

  public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
    EllHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository
      .getEllHeadersBySchoolId(sdcSchoolCollectionID);
    List<String> ellColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
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
        headcountHeader.getColumns()
          .put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder()
              .currentValue(String.valueOf(
                  Long.parseLong(result.getAllStudents())
                  - Long.parseLong(result.getReportedStudents()))).build());
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
