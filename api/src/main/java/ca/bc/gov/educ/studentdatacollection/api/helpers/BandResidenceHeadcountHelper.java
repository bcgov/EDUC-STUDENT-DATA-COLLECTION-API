package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class BandResidenceHeadcountHelper extends HeadcountHelper<BandResidenceHeadcountResult>{
    private final CodeTableService codeTableService;
    private static final String BAND_TITLE = "Indigenous Language and Culture";
    private static final String HEADCOUNT_TITLE = "Headcount";

    private static final String FTE_TITLE = "FTE";

    private static final String BAND_CODE = "bandCode";
    private static final List<String> HEADER_COLUMN_TITLES = List.of(BAND_TITLE, HEADCOUNT_TITLE, FTE_TITLE);

    private static final String ALL_TITLE = "All Bands & Students";

    protected List<BandCodeEntity> bandCodes;

    @Getter
    private static Map<String, String> bandRowTitles = new HashMap<>();


    public BandResidenceHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, CodeTableService codeTableService) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
        this.codeTableService = codeTableService;
        headcountMethods = getHeadcountMethods();
    }

    public List<HeadcountHeader> getHeadcountHeaders(UUID collectionID){
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        getBandTitles(collectionID);
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

    public void getBandTitles(UUID collectionID) {
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
        setBandRowTitles(rowTitleMap);
    }

    public HeadcountResultsTable convertBandHeadcountResults(List<BandResidenceHeadcountResult> results){
        HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
        headcountResultsTable.setHeaders(HEADER_COLUMN_TITLES);
        headcountResultsTable.setRows(new ArrayList<>());

        Double fteTotal = 0.0;
        Integer headcountTotal = 0;

        List <Map<String, String>> rows = new ArrayList<>();
        for(Map.Entry<String, String> title : bandRowTitles.entrySet()){
            Map<String, String> rowData = new LinkedHashMap<>();
            rowData.put("title", title.getValue());

            var result = results.stream()
                .filter(value -> value.getBandCode().equals(title.getKey()))
                    .findFirst()
                    .orElse(null);
            assert result != null;
            rowData.put(FTE_TITLE.toLowerCase(), result.getFteTotal());
            fteTotal += Double.parseDouble(result.getFteTotal());
            rowData.put(HEADCOUNT_TITLE.toLowerCase(), result.getHeadcount());
            headcountTotal += Integer.parseInt(result.getHeadcount());

            rows.add(rowData);
        }

        Map<String, String> totalRowData = new LinkedHashMap<>();
        totalRowData.put("title", ALL_TITLE);
        totalRowData.put(FTE_TITLE.toLowerCase(), fteTotal.toString());
        totalRowData.put(HEADCOUNT_TITLE.toLowerCase(), headcountTotal.toString());
        rows.add(totalRowData);

        headcountResultsTable.setRows(rows);
        return headcountResultsTable;
    }

    public void setBandRowTitles(Map<String, String> rowTitles) {
        this.bandRowTitles = rowTitles;
    }

    private Map<String, Function<BandResidenceHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<BandResidenceHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(FTE_TITLE.toLowerCase(), BandResidenceHeadcountResult::getFteTotal);
        headcountMethods.put(HEADCOUNT_TITLE.toLowerCase(), BandResidenceHeadcountResult :: getHeadcount);
        headcountMethods.put(BAND_CODE, BandResidenceHeadcountResult::getBandCode);

        return headcountMethods;
    }
}
