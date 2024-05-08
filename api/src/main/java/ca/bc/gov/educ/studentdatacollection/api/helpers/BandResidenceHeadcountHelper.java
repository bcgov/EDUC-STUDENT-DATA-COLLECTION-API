package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

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

    public void setBandResultsTableComparisonValuesDistrict(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, HeadcountResultsTable currentCollectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        List<BandResidenceHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertBandHeadcountResults(previousCollectionRawData, true);
        setResultsTableComparisonValues(currentCollectionData, previousCollectionData);
    }

    @Override
    public void setResultsTableComparisonValues(HeadcountResultsTable collectionData, HeadcountResultsTable previousCollectionData) {
        List<Map<String, HeadcountHeaderColumn>> rows = collectionData.getRows();
        List<Map<String, HeadcountHeaderColumn>> prevRows = previousCollectionData.getRows();
        IntStream.range(0, rows.size())
                .forEach(i -> {
                    var  currentData = collectionData.getRows().get(i);

                    currentData.forEach((rowName, currentRow) -> {
                        HeadcountHeaderColumn previousColumn =  prevRows.stream().flatMap(m -> m.entrySet().stream())
                                .filter(v -> v.getKey().equalsIgnoreCase(TITLE) && v.getValue().getCurrentValue().equalsIgnoreCase(currentRow.getCurrentValue()))
                                .map(Map.Entry::getValue)
                                .findFirst()
                                .orElse(null);

                        if(previousColumn != null) {
                            currentRow.setComparisonValue(previousColumn.getCurrentValue());
                        } else {
                            currentRow.setComparisonValue("0");
                        }
                    });
                });
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
        var schoolIdInSchoolCollection = result.stream().map(BandResidenceHeadcountResult::getSchoolID).toList();
        schoolIdInSchoolCollection.forEach(code -> {
            Optional<School> entity =  restUtils.getSchoolBySchoolID(code);
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
                var result = results.stream()
                        .filter(value -> (Boolean.TRUE.equals(schoolTitles) ? value.getSchoolID() : value.getBandCode()).equals(title.getKey()))
                        .findFirst()
                        .orElse(null);

                if (result != null && column.equalsIgnoreCase(FTE_TITLE)) {
                    var fteCurrentValue = StringUtils.isNotEmpty(result.getFteTotal()) ? result.getFteTotal(): "0";
                    fteTotal = fteTotal.add(new BigDecimal(fteCurrentValue));
                    rowData.put(column, HeadcountHeaderColumn.builder().currentValue(fteCurrentValue).build());
                } else if (result != null && column.equalsIgnoreCase(HEADCOUNT_TITLE)) {
                    var headcountCurrentValue = StringUtils.isNotEmpty(result.getHeadcount()) ? result.getHeadcount(): "0";
                    headcountTotal = headcountTotal.add(new BigDecimal(headcountCurrentValue));
                    rowData.put(column, HeadcountHeaderColumn.builder().currentValue(headcountCurrentValue).build());
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
}
