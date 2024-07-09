package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.BandResidenceHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BandResidenceHeadcountHelper extends HeadcountHelper<BandResidenceHeadcountResult>{
    private final CodeTableService codeTableService;
    private static final String TITLE = "title";
    private static final String HEADCOUNT_TITLE = "Headcount";

    private static final String FTE_TITLE = "FTE";

    private static final String BAND_CODE = "bandCode";
    private static final List<String> TABLE_COLUMN_TITLES = List.of(TITLE, HEADCOUNT_TITLE, FTE_TITLE);

    private static final String ALL_TITLE = "All Bands & Students";

    private static final Map<String, String> bandRowTitles = new HashMap<>();

    private final RestUtils restUtils;


    public BandResidenceHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository,
                                        CodeTableService codeTableService, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
        this.codeTableService = codeTableService;
        this.restUtils = restUtils;
        headcountMethods = getHeadcountMethods();
    }

    public void setBandResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable currentCollectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<BandResidenceHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcSchoolCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertBandHeadcountResults(previousCollectionRawData, false);
        setResultsTableComparisonValues(currentCollectionData, previousCollectionData);
    }

    public void setBandResultsTableComparisonValuesDistrict(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, HeadcountResultsTable currentCollectionData, Boolean schoolTitles) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<BandResidenceHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertBandHeadcountResults(previousCollectionRawData, schoolTitles);
        setResultsTableComparisonValues(currentCollectionData, previousCollectionData);
    }

    public void setBandTitles(List<BandResidenceHeadcountResult> result) {
        bandRowTitles.clear();
        var bandCodesInSchoolCollection = result.stream().map(BandResidenceHeadcountResult::getBandCode).toList();
        List<BandCodeEntity> allActiveBandCodes = codeTableService.getAllBandCodes();
        bandCodesInSchoolCollection.forEach(code -> {
            Optional<BandCodeEntity> entity = allActiveBandCodes.stream().filter(band -> band.getBandCode().equalsIgnoreCase(code)).findFirst();
            entity.ifPresent(bandCodeEntity -> bandRowTitles.put(code, code + " - " + bandCodeEntity.getLabel()));
        });
    }

    public void setSchoolTitles(List<BandResidenceHeadcountResult> result) {
        bandRowTitles.clear();
        var schoolIdInSchoolCollection = result.stream()
                .map(BandResidenceHeadcountResult::getSchoolID)
                .filter(Objects::nonNull)
                .toList();

        schoolIdInSchoolCollection.forEach(code -> {
            Optional<SchoolTombstone> entity = restUtils.getSchoolBySchoolID(code);
            entity.ifPresent(school -> bandRowTitles.put(code, school.getMincode() + " - " + school.getDisplayName()));
        });
    }

    public HeadcountResultsTable convertBandHeadcountResults(List<BandResidenceHeadcountResult> results, Boolean schoolTitles){
        HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
        headcountResultsTable.setHeaders(TABLE_COLUMN_TITLES);
        headcountResultsTable.setRows(new ArrayList<>());

        if (Boolean.TRUE.equals(schoolTitles)) {
            setSchoolTitles(results);
        } else {
            setBandTitles(results);
        }

        BigDecimal fteTotal = BigDecimal.ZERO;
        BigDecimal headcountTotal = BigDecimal.ZERO;
        List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
        List<String> columns = List.of(HEADCOUNT_TITLE, FTE_TITLE);
        for(Map.Entry<String, String> title : bandRowTitles.entrySet()){
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
            for (String column : columns) {
                var matchingResults = results.stream()
                        .filter(value -> {
                            String compareKey = Boolean.TRUE.equals(schoolTitles) ? value.getSchoolID() : value.getBandCode();
                            return compareKey.equals(title.getKey());
                        })
                        .toList();

                if (!matchingResults.isEmpty() && column.equalsIgnoreCase(FTE_TITLE)) {
                    var fteCurrentValue = matchingResults.stream()
                            .map(result -> StringUtils.isNotEmpty(result.getFteTotal()) ? new BigDecimal(result.getFteTotal()) : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    fteTotal = fteTotal.add(fteCurrentValue);
                    rowData.put(column, HeadcountHeaderColumn.builder().currentValue(fteCurrentValue.toString()).build());
                } else if (!matchingResults.isEmpty() && column.equalsIgnoreCase(HEADCOUNT_TITLE)) {
                    var headcountCurrentValue = matchingResults.stream()
                            .map(result -> StringUtils.isNotEmpty(result.getHeadcount()) ? new BigDecimal(result.getHeadcount()) : BigDecimal.ZERO)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    headcountTotal = headcountTotal.add(headcountCurrentValue);
                    rowData.put(column, HeadcountHeaderColumn.builder().currentValue(headcountCurrentValue.toString()).build());
                } else {
                    rowData.put(column, HeadcountHeaderColumn.builder().currentValue("0").build());
                }
            }
            rows.add(rowData);
        }

        Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
        totalRowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
        totalRowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue(fteTotal.toString()).build());
        totalRowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue(headcountTotal.toString()).build());
        rows.add(totalRowData);

        headcountResultsTable.setRows(rows);
        return headcountResultsTable;
    }

    private Map<String, Function<BandResidenceHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<BandResidenceHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(FTE_TITLE.toLowerCase(), BandResidenceHeadcountResult::getFteTotal);
        headcountMethods.put(HEADCOUNT_TITLE.toLowerCase(), BandResidenceHeadcountResult :: getHeadcount);
        headcountMethods.put(BAND_CODE, BandResidenceHeadcountResult::getBandCode);

        return headcountMethods;
    }

    @Override
    public void setResultsTableComparisonValues(HeadcountResultsTable currentCollectionData, HeadcountResultsTable previousCollectionData) {
        Map<String, Map<String, HeadcountHeaderColumn>> previousRowsMap = previousCollectionData.getRows().stream()
                .collect(Collectors.toMap(
                        row -> row.get(TITLE).getCurrentValue(),
                        Function.identity()
                ));

        for (Map<String, HeadcountHeaderColumn> currentRow : currentCollectionData.getRows()) {
            HeadcountHeaderColumn titleColumn = currentRow.get(TITLE);
            if (titleColumn != null && previousRowsMap.containsKey(titleColumn.getCurrentValue())) {
                Map<String, HeadcountHeaderColumn> previousRow = previousRowsMap.get(titleColumn.getCurrentValue());

                currentRow.forEach((key, currentColumn) -> {
                    if (previousRow.containsKey(key)) {
                        HeadcountHeaderColumn previousColumn = previousRow.get(key);
                        currentColumn.setComparisonValue(previousColumn.getCurrentValue());
                    } else {
                        currentColumn.setComparisonValue("0");
                    }
                });
            } else {
                currentRow.values().forEach(column -> column.setComparisonValue("0"));
            }
        }
    }
}
