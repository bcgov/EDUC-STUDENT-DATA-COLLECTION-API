package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class FrenchCombinedHeadcountHelper extends HeadcountHelper<FrenchCombinedHeadcountResult> {
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
    private static final String ALL_SCHOOLS = "All Schools";
    private static final String SECTION = "section";
    private static final String TITLE = "title";
    private static final String TOTAL = "Total";
    private final RestUtils restUtils;

    public FrenchCombinedHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
        this.restUtils = restUtils;
        headcountMethods = getHeadcountMethods();
        sectionTitles = getSelectionTitles();
        rowTitles = getRowTitles();
    }

    public void setGradeCodesForDistricts() {
        gradeCodes = SchoolGradeCodes.getNonIndependentKtoGAGrades();
    }

    public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);

        List<FrenchCombinedHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionId(previousCollectionID);
        compareWithPrevCollection(previousCollectionRawData, headcountHeaderList, collectionData, sdcDistrictCollectionEntity);
    }

    public void compareWithPrevCollection(List<FrenchCombinedHeadcountResult> previousCollectionRawData, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData, SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
        HeadcountResultsTable previousCollectionData = convertHeadcountResults(previousCollectionRawData);
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
        setResultsTableComparisonValues(collectionData, previousCollectionData);
    }

    public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<FrenchCombinedHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(previousCollectionID);

        HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawDataForHeadcount);
        List<HeadcountHeader> previousHeadcountHeaderList = this.getHeaders(previousCollectionID);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
        setResultsTableComparisonValues(collectionData, previousCollectionData);
    }

    public HeadcountResultsTable convertHeadcountResultsToSchoolGradeTable(List<FrenchCombinedHeadcountResult> results) throws EntityNotFoundException {
        HeadcountResultsTable table = new HeadcountResultsTable();
        List<String> headers = new ArrayList<>();
        Set<String> grades = new HashSet<>();
        Map<String, Map<String, Integer>> schoolGradeCounts = new HashMap<>();
        Map<String, Integer> totalCounts = new HashMap<>();
        Map<String, String> schoolDetails  = new HashMap<>();

        // Collect all grades and initialize school-grade map
        for (FrenchCombinedHeadcountResult result : results) {
            grades.add(result.getEnrolledGradeCode());
            schoolGradeCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
            schoolDetails .putIfAbsent(result.getSchoolID(),
                    restUtils.getSchoolBySchoolID(result.getSchoolID())
                            .map(school -> school.getMincode() + " - " + school.getDisplayName())
                            .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", result.getSchoolID())));
        }

        // Initialize totals for each grade
        for (String grade : gradeCodes) {
            totalCounts.put(grade, 0);
            schoolGradeCounts.values().forEach(school -> school.putIfAbsent(grade, 0));
        }

        // Sort grades and add to headers
        List<String> sortedGrades = new ArrayList<>(grades);
        Collections.sort(sortedGrades);
        headers.add(TITLE);
        headers.addAll(sortedGrades);
        headers.add(TOTAL);
        table.setHeaders(headers);

        // Populate counts for each school and grade
        Map<String, Integer> schoolTotals = new HashMap<>();
        for (FrenchCombinedHeadcountResult result : results) {
            Map<String, Integer> gradeCounts = schoolGradeCounts.get(result.getSchoolID());
            String grade = result.getEnrolledGradeCode();
            int count = getCountFromResult(result);
            gradeCounts.merge(grade, count, Integer::sum);
            totalCounts.merge(grade, count, Integer::sum);
            schoolTotals.merge(result.getSchoolID(), count, Integer::sum);
        }

        List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

        // Add all schools row at the start
        Map<String, HeadcountHeaderColumn> totalRow = new LinkedHashMap<>();
        totalRow.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
        totalRow.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
        totalCounts.forEach((grade, count) -> totalRow.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
        totalRow.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.values().stream().mapToInt(Integer::intValue).sum())).build());
        rows.add(totalRow);

        // Create rows for the table, including school names
        schoolGradeCounts.forEach((schoolID, gradesCount) -> {
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolDetails.get(schoolID)).build());
            rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
            gradesCount.forEach((grade, count) -> rowData.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
            rowData.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.get(schoolID))).build());
            rows.add(rowData);
        });

        table.setRows(rows);
        return table;
    }


    private int getCountFromResult(FrenchCombinedHeadcountResult result) {
        try {
            return Integer.parseInt(result.getTotalTotals());
        } catch (NumberFormatException e) {
            log.error("Error parsing count from result for SchoolID {}: {}", result.getSchoolID(), e.getMessage());
            return 0;
        }
    }

    public List<HeadcountHeader> getHeaders(UUID sdcDistrictCollectionID) {
        FrenchCombinedHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getFrenchHeadersByDistrictId(sdcDistrictCollectionID);
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

    private Map<String, Function<FrenchCombinedHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<FrenchCombinedHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(CORE_TOTAL_TITLE, FrenchCombinedHeadcountResult::getTotalCoreFrench);
        headcountMethods.put(CORE_SCHOOL_AGE_TITLE, FrenchCombinedHeadcountResult::getSchoolAgedCoreFrench);
        headcountMethods.put(CORE_ADULT_TITLE, FrenchCombinedHeadcountResult::getAdultCoreFrench);
        headcountMethods.put(EARLY_TOTAL_TITLE, FrenchCombinedHeadcountResult::getTotalEarlyFrench);
        headcountMethods.put(EARLY_SCHOOL_AGE_TITLE, FrenchCombinedHeadcountResult::getSchoolAgedEarlyFrench);
        headcountMethods.put(EARLY_ADULT_TITLE, FrenchCombinedHeadcountResult::getAdultEarlyFrench);
        headcountMethods.put(LATE_TOTAL_TITLE, FrenchCombinedHeadcountResult::getTotalLateFrench);
        headcountMethods.put(LATE_SCHOOL_AGE_TITLE, FrenchCombinedHeadcountResult::getSchoolAgedLateFrench);
        headcountMethods.put(LATE_ADULT_TITLE, FrenchCombinedHeadcountResult::getAdultLateFrench);
        headcountMethods.put(FRANCO_TOTAL_TITLE, FrenchCombinedHeadcountResult::getTotalFrancophone);
        headcountMethods.put(FRANCO_SCHOOL_AGE_TITLE, FrenchCombinedHeadcountResult::getSchoolAgedFrancophone);
        headcountMethods.put(FRANCO_ADULT_TITLE, FrenchCombinedHeadcountResult::getAdultFrancophone);
        headcountMethods.put(ALL_TOTAL_TITLE, FrenchCombinedHeadcountResult::getTotalTotals);
        headcountMethods.put(ALL_SCHOOL_AGE_TITLE, FrenchCombinedHeadcountResult::getSchoolAgedTotals);
        headcountMethods.put(ALL_ADULT_TITLE, FrenchCombinedHeadcountResult::getAdultTotals);
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
