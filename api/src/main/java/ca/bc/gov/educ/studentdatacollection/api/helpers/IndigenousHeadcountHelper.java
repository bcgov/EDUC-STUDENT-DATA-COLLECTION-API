package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class IndigenousHeadcountHelper extends HeadcountHelper<IndigenousHeadcountResult>{

    private static final String INDIGENOUS_LANGUAGE_TITLE = "Indigenous Language and Culture";
    private static final String INDIGENOUS_SUPPORT_TITLE = "Indigenous Support Services";
    private static final String OTHER_APPROVED_TITLE = "Other Approved Indigenous Programs";
    private static final String ANCESTRY_COUNT_TITLE = "Indigenous Ancestry";
    private static final String LIVING_ON_RESERVE_TITLE = "Ordinarily Living on Reserve";
    private static final String ALL_TITLE = "All Indigenous Support Programs";
    private static final String ELIGIBLE_TITLE = "Eligible";
    private static final String REPORTED_TITLE = "Reported";
    private static final String NOT_REPORTED_TITLE = "Not Reported";
    private static final String TOTAL_STUDENTS = "Total Students";
    private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
    private static final List<String> HEADER_COLUMN_TITLE = List.of(TOTAL_STUDENTS);
    private static final String LANGUAGE_TOTAL_KEY = "languageTotal";
    private static final String SUPPORT_TOTAL_KEY = "supportTotal";
    private static final String OTHER_TOTAL_KEY = "otherTotal";
    private static final String ALL_TOTAL_KEY = "coopXG";

    public IndigenousHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
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

    public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    }

    public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<IndigenousHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
        setResultsTableComparisonValues(collectionData, previousCollectionData);
    }

    public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
        IndigenousHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getIndigenousHeadersBySchoolId(sdcSchoolCollectionID);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        Arrays.asList(INDIGENOUS_LANGUAGE_TITLE, INDIGENOUS_SUPPORT_TITLE, OTHER_APPROVED_TITLE, ANCESTRY_COUNT_TITLE, LIVING_ON_RESERVE_TITLE).forEach(headerTitle -> {
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);
            switch (headerTitle) {
                case INDIGENOUS_LANGUAGE_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getEligIndigenousLanguage())).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedIndigenousLanguage())).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedIndigenousLanguage()))).build());
                }
                case INDIGENOUS_SUPPORT_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligIndigenousSupport()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedIndigenousSupport()).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedIndigenousSupport()))).build());
                }
                case OTHER_APPROVED_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligOtherProgram()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedOtherProgram()).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedOtherProgram()))).build());
                }
                case ANCESTRY_COUNT_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLE);
                    headcountHeader.getColumns().put(TOTAL_STUDENTS, HeadcountHeaderColumn.builder().currentValue(result.getStudentsWithIndigenousAncestry()).build());
                }
                case LIVING_ON_RESERVE_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLE);
                    headcountHeader.getColumns().put(TOTAL_STUDENTS, HeadcountHeaderColumn.builder().currentValue(result.getStudentsWithFundingCode20()).build());
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

    @Override
    public void setComparisonValues(List<HeadcountHeader> headcountHeaderList, List<HeadcountHeader> previousHeadcountHeaderList) {
        IntStream.range(0, headcountHeaderList.size())
                .forEach(i -> {
                    HeadcountHeader currentHeader = headcountHeaderList.get(i);
                    HeadcountHeader previousHeader = previousHeadcountHeaderList.get(i);

                    currentHeader.getColumns().forEach((columnName, currentColumn) -> {
                        HeadcountHeaderColumn previousColumn = previousHeader.getColumns().get(columnName);
                        currentColumn.setComparisonValue(previousColumn.getCurrentValue());
                    });

                    if(currentHeader.getHeadCountValue() != null && previousHeader.getHeadCountValue() != null) {
                        currentHeader.getHeadCountValue().setComparisonValue(previousHeader.getHeadCountValue().getCurrentValue());
                    }
                });
    }

    private Map<String, Function<IndigenousHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<IndigenousHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(LANGUAGE_TOTAL_KEY, IndigenousHeadcountResult::getIndigenousLanguageTotal);
        headcountMethods.put(SUPPORT_TOTAL_KEY, IndigenousHeadcountResult::getIndigenousSupportTotal);
        headcountMethods.put(OTHER_TOTAL_KEY, IndigenousHeadcountResult::getOtherProgramTotal);
        headcountMethods.put(ALL_TOTAL_KEY, IndigenousHeadcountResult::getAllSupportProgramTotal);
        return headcountMethods;
    }
    private Map<String, String> getSelectionTitles() {
        Map<String, String> sectionTitles = new HashMap<>();

        sectionTitles.put(LANGUAGE_TOTAL_KEY, INDIGENOUS_LANGUAGE_TITLE);
        sectionTitles.put(SUPPORT_TOTAL_KEY, INDIGENOUS_SUPPORT_TITLE);
        sectionTitles.put(OTHER_TOTAL_KEY, OTHER_APPROVED_TITLE);
        sectionTitles.put(ALL_TOTAL_KEY, ALL_TITLE);
        return sectionTitles;
    }
    private Map<String, String> getRowTitles() {
        Map<String, String> rowTitles = new LinkedHashMap<>();

        rowTitles.put(LANGUAGE_TOTAL_KEY, INDIGENOUS_LANGUAGE_TITLE);
        rowTitles.put(SUPPORT_TOTAL_KEY, INDIGENOUS_SUPPORT_TITLE);
        rowTitles.put(OTHER_TOTAL_KEY, OTHER_APPROVED_TITLE);
        rowTitles.put(ALL_TOTAL_KEY, ALL_TITLE);
        return rowTitles;
    }
}
