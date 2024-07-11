package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
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
    private static final String TOTAL_STUDENTS = "Total Students";
    private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
    private static final List<String> HEADER_COLUMN_TITLE = List.of(TOTAL_STUDENTS);
    private static final String LANGUAGE_TOTAL_KEY = "languageTotal";
    private static final String SUPPORT_TOTAL_KEY = "supportTotal";
    private static final String OTHER_TOTAL_KEY = "otherTotal";
    private static final String ALL_TOTAL_KEY = "coopXG";
    private static final String ALL_SCHOOLS = "All Schools";
    private static final String SECTION = "section";
    private static final String TITLE = "title";
    private static final String TOTAL = "Total";
    private final RestUtils restUtils;

    public IndigenousHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
                                     SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
        this.restUtils = restUtils;
        headcountMethods = getHeadcountMethods();
        sectionTitles = getSelectionTitles();
        rowTitles = getRowTitles();
    }

    public void setGradeCodes(Optional<SchoolTombstone> school) {
      if(school.isPresent() && (school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode()) || school.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode()))) {
        gradeCodes = SchoolGradeCodes.getIndependentKtoSUGrades();
      } else {
        gradeCodes = SchoolGradeCodes.getNonIndependentKtoSUGrades();
      }
    }

    public void setGradeCodesForDistricts() {
        gradeCodes = SchoolGradeCodes.getNonIndependentKtoSUGrades();
    }

    public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, false);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    }

    public void setComparisonValuesForDistrict(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    }

    public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<IndigenousHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
        setResultsTableComparisonValues(collectionData, previousCollectionData);
    }

    public void setResultsTableComparisonValuesForDistrict(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<IndigenousHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
        setResultsTableComparisonValues(collectionData, previousCollectionData);
    }

    public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<IndigenousHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
        setResultsTableComparisonValuesDynamic(collectionData, previousCollectionData);
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

    public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID, Boolean isDistrict) {
        IndigenousHeadcountHeaderResult result = (Boolean.TRUE.equals(isDistrict))
                ? sdcSchoolCollectionStudentRepository.getIndigenousHeadersByDistrictId(sdcSchoolCollectionID)
                : sdcSchoolCollectionStudentRepository.getIndigenousHeadersBySchoolId(sdcSchoolCollectionID);
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
                }
                case INDIGENOUS_SUPPORT_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligIndigenousSupport()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedIndigenousSupport()).build());
                }
                case OTHER_APPROVED_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligOtherProgram()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedOtherProgram()).build());
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

    public HeadcountResultsTable convertHeadcountResultsToSchoolGradeTable(UUID sdcDistrictCollectionID, List<IndigenousHeadcountResult> results) throws EntityNotFoundException {
        HeadcountResultsTable table = new HeadcountResultsTable();
        List<String> headers = new ArrayList<>();
        Set<String> grades = new HashSet<>(gradeCodes);
        Map<String, Map<String, Integer>> schoolGradeCounts = new HashMap<>();
        Map<String, Integer> totalCounts = new HashMap<>();
        Map<String, String> schoolDetails  = new HashMap<>();

        List<SdcSchoolCollectionEntity> allSchoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
        List<SchoolTombstone> allSchools = allSchoolCollections.stream()
                .map(schoolCollection -> restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString())
                .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", schoolCollection.getSchoolID().toString())))
                .toList();

        // Collect all grades and initialize school-grade map
        for (IndigenousHeadcountResult result : results) {
            grades.add(result.getEnrolledGradeCode());
            schoolGradeCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
            schoolDetails.putIfAbsent(result.getSchoolID(),
                    restUtils.getSchoolBySchoolID(result.getSchoolID())
                            .map(school -> school.getMincode() + " - " + school.getDisplayName())
                            .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", result.getSchoolID())));
        }

        for (SchoolTombstone school : allSchools) {
            schoolGradeCounts.computeIfAbsent(school.getSchoolId(), k -> new HashMap<>());
            schoolDetails.putIfAbsent(school.getSchoolId(), school.getMincode() + " - " + school.getDisplayName());
        }

        // Initialize totals for each grade
        grades.forEach(grade -> {
            totalCounts.put(grade, 0);
            schoolGradeCounts.values().forEach(school -> school.putIfAbsent(grade, 0));
        });

        // Sort grades and add to headers
        headers.add(TITLE);
        headers.addAll(grades);
        headers.add(TOTAL);
        table.setHeaders(headers);

        // Populate counts for each school and grade, and calculate row totals
        Map<String, Integer> schoolTotals = new HashMap<>();
        for (IndigenousHeadcountResult result : results) {
            if (grades.contains(result.getEnrolledGradeCode())) {
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
        int allSchoolsTotal = schoolTotals.values().stream().mapToInt(Integer::intValue).sum();
        totalRow.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(allSchoolsTotal)).build());
        rows.add(totalRow);

        // Create rows for the table, including school names
        schoolGradeCounts.forEach((schoolID, gradesCount) -> {
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolDetails.get(schoolID)).build());
            rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
            gradesCount.forEach((grade, count) -> rowData.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
            int schoolTotal = gradesCount.values().stream().mapToInt(Integer::intValue).sum();
            rowData.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotal)).build());
            rows.add(rowData);
        });

        table.setRows(rows);
        return table;
    }

    private int getCountFromResult(IndigenousHeadcountResult result) {
        return Optional.ofNullable(result.getAllSupportProgramTotal()).map(Integer::parseInt).orElse(0);
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
