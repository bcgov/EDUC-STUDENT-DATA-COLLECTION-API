package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.Setter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class BandResidenceHeadcountHelper extends HeadcountHelper<BandResidenceHeadcountResult>{
    private final CodeTableService codeTableService;
    private static final String BAND_TITLE = "Indigenous Language and Culture";
    private static final String HEADCOUNT_TITLE = "Headcount";

    private static final String FTE_TITLE = "FTE";

    private static final String BAND_CODE = "bandCode";
    private static final List<String> HEADER_COLUMN_TITLES = List.of(BAND_TITLE, HEADCOUNT_TITLE, FTE_TITLE);

    private static final String ALL_TITLE = "All Bands & Students";


    @Setter
    @Getter
    private static Map<String, String> bandRowTitles = new HashMap<>();


    public BandResidenceHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, CodeTableService codeTableService) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
        this.codeTableService = codeTableService;
        headcountMethods = getHeadcountMethods();
    }

    public List<HeadcountHeader> getHeadcountHeaders(List<UUID> collectionIDs){
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        collectionIDs.forEach(collectionID -> bandRowTitles.putAll(getBandTitles(collectionID)));

        List<String> bandTitles = getBandRowTitles().values().stream().toList();
        bandTitles.forEach(headerTitle ->{
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);
            headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
            headcountHeaderList.add(headcountHeader);
        });
        return headcountHeaderList;
    }

    public Map<String, String> getBandTitles(UUID collectionID) {
        List<String> bandCodesInCollection = sdcSchoolCollectionStudentRepository.getBandResidenceCodesByCollectionId(UUID.fromString(String.valueOf(collectionID)));
        List<BandCodeEntity> allActiveBandCodes = codeTableService.getAllBandCodes();
        Map<String, String> rowTitleMap = new LinkedHashMap<>();
        Map<String, BandCodeEntity> bandCodeMap = allActiveBandCodes.stream()
                .collect(Collectors.toMap(BandCodeEntity::getBandCode, entity -> entity));
        bandCodesInCollection.forEach(code -> {
            BandCodeEntity entity = bandCodeMap.get(code);
            if (entity != null) {
                rowTitleMap.put(code, code + " - " + entity.getLabel());
            }
        });
        return rowTitleMap;
    }

    public HeadcountResultsTable convertBandHeadcountResults(List<BandResidenceHeadcountResult> results){
        HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
        headcountResultsTable.setHeaders(HEADER_COLUMN_TITLES);
        headcountResultsTable.setRows(new ArrayList<>());

        Double fteTotal = 0.0;
        Integer headcountTotal = 0;

        List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
        for(Map.Entry<String, String> title : bandRowTitles.entrySet()){
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put("title", HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());

            var result = results.stream()
                .filter(value -> value.getBandCode().equals(title.getKey()))
                    .findFirst()
                    .orElse(null);
            if(result != null) {
                rowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getFteTotal()).build());
                fteTotal += Double.parseDouble(result.getFteTotal());
                rowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getHeadcount()).build());
                headcountTotal += Integer.parseInt(result.getHeadcount());
            } else {
                rowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue("0").build());
                rowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue("0").build());
            }

            rows.add(rowData);
        }

        Map<String, HeadcountHeaderColumn> totalRowData = new LinkedHashMap<>();
        totalRowData.put("title", HeadcountHeaderColumn.builder().currentValue(ALL_TITLE).build());
        totalRowData.put(FTE_TITLE, HeadcountHeaderColumn.builder().currentValue(fteTotal.toString()).build());
        totalRowData.put(HEADCOUNT_TITLE, HeadcountHeaderColumn.builder().currentValue(headcountTotal.toString()).build());
        rows.add(totalRowData);

        headcountResultsTable.setRows(rows);
        return headcountResultsTable;
    }

    public void setBandResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable currentCollectionData) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<BandResidenceHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySchoolId(previousCollectionID);
        HeadcountResultsTable previousCollectionData = convertBandHeadcountResults(previousCollectionRawData);
        setResultsTableComparisonValues(currentCollectionData, previousCollectionData);
    }


    private Map<String, Function<BandResidenceHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<BandResidenceHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(FTE_TITLE.toLowerCase(), BandResidenceHeadcountResult::getFteTotal);
        headcountMethods.put(HEADCOUNT_TITLE.toLowerCase(), BandResidenceHeadcountResult :: getHeadcount);
        headcountMethods.put(BAND_CODE, BandResidenceHeadcountResult::getBandCode);

        return headcountMethods;
    }
}
