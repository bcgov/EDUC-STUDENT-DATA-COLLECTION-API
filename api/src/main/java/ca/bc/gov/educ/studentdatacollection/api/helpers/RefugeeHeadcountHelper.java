package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
@Slf4j
public class RefugeeHeadcountHelper extends HeadcountHelper<RefugeeHeadcountResult> {

    // Header Titles
    private static final String REFUGEE_TITLE = "Newcomer Refugees";
    private static final String ELIGIBLE_TITLE = "Eligible";
    private static final String REPORTED_TITLE = "Reported";

    // Table Titles
    private static final String TITLE = "title";
    private static final String HEADCOUNT_TITLE = "Headcount";
    private static final String FTE_TITLE = "FTE";
    private static final String ELL_TITLE = "ELL";
    private static final List<String> TABLE_COLUMN_TITLES = List.of(TITLE, HEADCOUNT_TITLE, FTE_TITLE, ELL_TITLE);
    private static final String ALL_TITLE = "All Newcomer Refugees";
    public static final String SECTION="section";

    private static final Map<String, String> refugeeRowTitles = new HashMap<>();

    public RefugeeHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
    }

    public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID, boolean isDistrict) {
        RefugeeHeadcountHeaderResult result = isDistrict
                ? sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(sdcSchoolCollectionID)
                : sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySchoolId(sdcSchoolCollectionID);

        List<String> refugeeColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();

        List.of(REFUGEE_TITLE).forEach(headerTitle -> {
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);

            if (StringUtils.equals(headerTitle, REFUGEE_TITLE)) {
                headcountHeader.setOrderedColumnTitles(refugeeColumnTitles);
                headcountHeader.getColumns()
                        .put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder()
                                .currentValue(String.valueOf(result.getEligibleStudents())).build());
                headcountHeader.getColumns()
                        .put(REPORTED_TITLE, HeadcountHeaderColumn.builder()
                                .currentValue(String.valueOf(result.getReportedStudents())).build());
            } else { log.warn("Unexpected case headerTitle.  This should not have happened."); }

            headcountHeaderList.add(headcountHeader);
        });
        return headcountHeaderList;
    }

    public HeadcountResultsTable convertRefugeeHeadcountResults(UUID sdcDistrictCollectionID, List<RefugeeHeadcountResult> results) {
        HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
        headcountResultsTable.setHeaders(TABLE_COLUMN_TITLES);
        headcountResultsTable.setRows(new ArrayList<>());
        setSchoolTitles(sdcDistrictCollectionID);

        BigDecimal fteTotal = BigDecimal.ZERO;
        BigDecimal headcountTotal = BigDecimal.ZERO;
        BigDecimal ellTotal = BigDecimal.ZERO;
        List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

        for(Map.Entry<String, String> title : refugeeRowTitles.entrySet()){
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
            rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
            List<RefugeeHeadcountResult> matchingResults = results.stream()
                    .filter(result -> result.getSchoolID().equals(title.getKey()))
                    .toList();

            if (!matchingResults.isEmpty()) {
                BigDecimal fteCurrentValue = matchingResults.stream()
                        .map(result -> new BigDecimal(result.getFteTotal()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                rowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue(fteCurrentValue.toString()).build());
                rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
                fteTotal = fteTotal.add(fteCurrentValue);

                BigDecimal headcountCurrentValue = matchingResults.stream()
                        .map(result -> new BigDecimal(result.getHeadcount()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                rowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue(headcountCurrentValue.toString()).build());
                rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
                headcountTotal = headcountTotal.add(headcountCurrentValue);

                BigDecimal ellCurrentValue = matchingResults.stream()
                        .map(result -> new BigDecimal(result.getEll()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                rowData.put(ELL_TITLE, HeadcountHeaderColumn.builder().currentValue(ellCurrentValue.toString()).build());
                rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
                ellTotal = ellTotal.add(ellCurrentValue);
            } else {
                rowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue("0").build());
                rowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue("0").build());
                rowData.put(ELL_TITLE, HeadcountHeaderColumn.builder().currentValue("0").build());
                rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
            }

            rows.add(rowData);
        }

        Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
        totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
        totalRowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue(fteTotal.toString()).build());
        totalRowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue(headcountTotal.toString()).build());
        totalRowData.put(ELL_TITLE, HeadcountHeaderColumn.builder().currentValue(ellTotal.toString()).build());
        totalRowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
        rows.add(0, totalRowData);

        headcountResultsTable.setRows(rows);
        return headcountResultsTable;
    }

    public void setSchoolTitles(UUID sdcDistrictCollectionID) {
        refugeeRowTitles.clear();

        List<SchoolTombstone> allSchools = getAllSchoolTombstones(sdcDistrictCollectionID);

        allSchools.forEach(school -> refugeeRowTitles.put(school.getSchoolId(), school.getMincode() + " - " + school.getDisplayName()));
    }
}
