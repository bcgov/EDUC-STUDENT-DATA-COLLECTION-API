package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ZeroFteReasonCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.ZeroFTEHeadcountResult;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ZeroFTEHeadcountHelper extends HeadcountHelper<ZeroFTEHeadcountResult> {

    // Header Titles
    private static final String NON_FUNDED_STUDENTS_KEY = "nonfundedstudents";
    private static final String NON_FUNDED_STUDENTS_TITLE = "Non-Funded Students";
    private static final String TOTAL_NON_FUNDED_STUDENTS_TITLE = "Total Non-Funded Students";
    private static final String TOTAL_TITLE = "Total";
    private static final String TITLE = "title";
    private final RestUtils restUtils;

    public ZeroFTEHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
                                  SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
        this.restUtils = restUtils;
        headcountMethods = getHeadcountMethods();
        sectionTitles = getSelectionTitles();
        rowTitles = getRowTitles();
        gradeCodes = SchoolGradeCodes.getNonIndependentSchoolGrades();
    }

    /**
     * Exposes the SchoolGradeCodes for usage in pdf generation.
     *
     * @return Collection of all codes.
     */
    public List<String> getGradeCodesForDistricts() {
        return gradeCodes;
    }

    /**
     * Executor method for report datatable
     *
     * @param results List of ZeroFTEHeadcountResult
     * @return HeadcountResultsTable
     */
    public HeadcountResultsTable convertFteZeroHeadcountResults(List<ZeroFTEHeadcountResult> results) {
        HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
        List<String> headers = prepareHeaders();
        //Attach Headers
        headcountResultsTable.setHeaders(headers);
        //Prepare HeadCount Dataset
        List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
        Map<String, String> fteReasonDetails = getZeroFTEReasonCodes();
        Map<String, HeadcountHeaderColumn> titleRow = new LinkedHashMap<>();
        titleRow.put(TITLE, HeadcountHeaderColumn.builder().currentValue(NON_FUNDED_STUDENTS_TITLE).build());
        titleRow.put(SECTION, HeadcountHeaderColumn.builder().currentValue(NON_FUNDED_STUDENTS_TITLE).build());
        rows.add(titleRow);
        fteReasonDetails.forEach((code, message) -> {
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(message).build());
            rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(NON_FUNDED_STUDENTS_TITLE).build());
            rowData.putAll(prepareFTEGradeRowData(results, code));
            rows.add(rowData);
        });
        //Prepare Footer: grade-wise total
        createTotalSection(rows, results);
        //Add all rows to report table
        headcountResultsTable.setRows(rows);
        return headcountResultsTable;
    }

    /**
     * This method helps in preparing header datatable for data comparison
     *
     * @param headcountResultsTable HeadcountResultsTable
     * @return List of HeadcountResultsTable
     */
    public List<HeadcountHeader> getHeaders(HeadcountResultsTable headcountResultsTable) {
        List<String> gradeColumnTitles = new ArrayList<>(gradeCodes);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        HeadcountHeader headcountHeader = new HeadcountHeader();
        headcountHeader.setColumns(new HashMap<>());
        headcountHeader.setTitle(NON_FUNDED_STUDENTS_TITLE);
        headcountHeader.setOrderedColumnTitles(gradeColumnTitles);
        for (String gradeCode : gradeCodes) {
            HeadcountHeaderColumn headcountHeaderColumn = new HeadcountHeaderColumn();
            headcountHeaderColumn.setCurrentValue(String.valueOf(
                    headcountResultsTable.getRows().stream()
                            .filter(row -> row.get(TITLE).getCurrentValue().equals(TOTAL_NON_FUNDED_STUDENTS_TITLE) && row.get(SECTION).getCurrentValue().equals(TOTAL_NON_FUNDED_STUDENTS_TITLE))
                            .mapToLong(row -> Long.parseLong(row.get(gradeCode).getCurrentValue()))
                            .sum()));
            headcountHeader.getColumns().put(gradeCode, headcountHeaderColumn);
        }
        headcountHeaderList.add(headcountHeader);
        return headcountHeaderList;
    }

    /**
     * This method does the comparison in report datatable.
     *
     * @param sdcDistrictCollectionEntity Entity reference to get the previous collection data
     * @param headcountHeaderList         List of HeadcountHeader
     * @param collectionData              Current collection data
     */
    public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<ZeroFTEHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getZeroFTEHeadcountsBySdcDistrictCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertFteZeroHeadcountResults(previousCollectionRawData);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionData);
        setResultsTableComparisonValuesDynamic(collectionData, previousCollectionData);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    }

    /**
     * This method helps in preparing the header of report.
     *
     * @return List of headers
     */
    private List<String> prepareHeaders() {
        List<String> headers = new ArrayList<>();
        List<String> grades = new ArrayList<>(gradeCodes);
        headers.add(TITLE);
        headers.addAll(grades);
        headers.add(TOTAL_TITLE);
        return headers;
    }

    /**
     * This method sums up the grade-wise headcounts and add to report rows.
     *
     * @param rows    Collection of report rows
     * @param results List of ZeroFTEHeadcountResult
     */
    private void createTotalSection(List<Map<String, HeadcountHeaderColumn>> rows, List<ZeroFTEHeadcountResult> results) {
        Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
        BigDecimal sectionTotal = BigDecimal.ZERO;

        totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(TOTAL_NON_FUNDED_STUDENTS_TITLE).build());
        totalRowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(TOTAL_NON_FUNDED_STUDENTS_TITLE).build());
        for (String gradeCode : gradeCodes) {
            int totalHeadcountPerGrade = results.stream().mapToInt(result -> Integer.parseInt(getHeadCountValue(result, gradeCode))).sum();
            totalRowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(String.valueOf(totalHeadcountPerGrade)).build());
            sectionTotal = sectionTotal.add(new BigDecimal(totalHeadcountPerGrade));
        }
        totalRowData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(sectionTotal)).build());
        rows.add(totalRowData);
    }

    /**
     * This returns all the Zero-FTE maintained reasons and other.
     *
     * @return Map of code as key and description message as value.
     */
    public Map<String, String> getZeroFTEReasonCodes() {
        Map<String, String> fteReasonDetails = new HashMap<>();
        for (ZeroFteReasonCodes reasonCode : ZeroFteReasonCodes.values()) {
            fteReasonDetails.put(reasonCode.getCode(), reasonCode.getMessage());
        }
        fteReasonDetails.put("Other", "Other");
        return fteReasonDetails;
    }

    private Map<String, String> getSelectionTitles() {
        return Map.of(TOTAL_TITLE, NON_FUNDED_STUDENTS_TITLE);
    }

    private Map<String, String> getRowTitles() {
        Map<String, String> rowTitles = new LinkedHashMap<>();
        rowTitles.put(NON_FUNDED_STUDENTS_KEY, NON_FUNDED_STUDENTS_TITLE);
        return rowTitles;
    }

    private Map<String, HeadcountHeaderColumn> prepareFTEGradeRowData(List<ZeroFTEHeadcountResult> results, String reasonCode) {
        Map<String, HeadcountHeaderColumn> gradeData = new LinkedHashMap<>();
        ZeroFTEHeadcountResult matchedResult = results.stream()
                .filter(result -> reasonCode.equals(result.getFteZeroReasonCode()))
                .findFirst()
                .orElse(null);
        for (String grade : gradeCodes) {
            gradeData.put(grade, HeadcountHeaderColumn.builder().currentValue(getHeadCountValue(matchedResult, grade)).build());
        }
        gradeData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(matchedResult != null ? matchedResult.getAllLevels() : "0").build());
        return gradeData;
    }

    /**
     * Helper method to get  value for grade from ZeroFTEHeadcountResult
     *
     * @param result    ZeroFTEHeadcountResult
     * @param gradeCode GradeCode
     * @return HeadCount Value
     */
    public String getHeadCountValue(ZeroFTEHeadcountResult result, String gradeCode) {
        return result != null && this.headcountMethods.containsKey(gradeCode) ? this.headcountMethods.get(gradeCode).apply(result) : "0";
    }

    /**
     * Supportive method to set and get value for report data.
     *
     * @return Map of gradeCode as Key and function as value for data lookup.
     */
    private Map<String, Function<ZeroFTEHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<ZeroFTEHeadcountResult, String>> headcountMethods = new HashMap<>();
        headcountMethods.put(SchoolGradeCodes.KINDFULL.getCode(), ZeroFTEHeadcountResult::getGradeKF);
        headcountMethods.put(SchoolGradeCodes.GRADE01.getCode(), ZeroFTEHeadcountResult::getGrade01);
        headcountMethods.put(SchoolGradeCodes.GRADE02.getCode(), ZeroFTEHeadcountResult::getGrade02);
        headcountMethods.put(SchoolGradeCodes.GRADE03.getCode(), ZeroFTEHeadcountResult::getGrade03);
        headcountMethods.put(SchoolGradeCodes.GRADE04.getCode(), ZeroFTEHeadcountResult::getGrade04);
        headcountMethods.put(SchoolGradeCodes.GRADE05.getCode(), ZeroFTEHeadcountResult::getGrade05);
        headcountMethods.put(SchoolGradeCodes.GRADE06.getCode(), ZeroFTEHeadcountResult::getGrade06);
        headcountMethods.put(SchoolGradeCodes.GRADE07.getCode(), ZeroFTEHeadcountResult::getGrade07);
        headcountMethods.put(SchoolGradeCodes.ELEMUNGR.getCode(), ZeroFTEHeadcountResult::getGradeEU);
        headcountMethods.put(SchoolGradeCodes.GRADE08.getCode(), ZeroFTEHeadcountResult::getGrade08);
        headcountMethods.put(SchoolGradeCodes.GRADE09.getCode(), ZeroFTEHeadcountResult::getGrade09);
        headcountMethods.put(SchoolGradeCodes.GRADE10.getCode(), ZeroFTEHeadcountResult::getGrade10);
        headcountMethods.put(SchoolGradeCodes.GRADE11.getCode(), ZeroFTEHeadcountResult::getGrade11);
        headcountMethods.put(SchoolGradeCodes.GRADE12.getCode(), ZeroFTEHeadcountResult::getGrade12);
        headcountMethods.put(SchoolGradeCodes.SECONDARY_UNGRADED.getCode(), ZeroFTEHeadcountResult::getGradeSU);
        headcountMethods.put(SchoolGradeCodes.GRADUATED_ADULT.getCode(), ZeroFTEHeadcountResult::getGradeGA);
        headcountMethods.put(SchoolGradeCodes.HOMESCHOOL.getCode(), ZeroFTEHeadcountResult::getGradeHS);
        headcountMethods.put(TOTAL_TITLE, ZeroFTEHeadcountResult::getAllLevels);
        return headcountMethods;
    }

}
