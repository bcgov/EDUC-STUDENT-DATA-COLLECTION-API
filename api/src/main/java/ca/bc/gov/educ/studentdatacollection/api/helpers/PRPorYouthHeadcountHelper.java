package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
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
public class PRPorYouthHeadcountHelper extends HeadcountHelper<PRPorYouthHeadcountResult> {
    private static final String YOUTH_TITLE = "Youth Custody";
    private static final String SHORT_PRP_TITLE = "Short PRP";
    private static final String LONG_PRP_TITLE = "Long PRP";
    private static final String ALL_TITLE = "Total";
    private static final String REPORTED_TITLE = "Reported";
    private static final String ALL_SCHOOLS = "All Schools";
    private static final String SECTION = "section";
    private static final String TITLE = "title";
    private static final String TOTAL = "Total";

    private final RestUtils restUtils;

    public PRPorYouthHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
        this.restUtils = restUtils;
        headcountMethods = getHeadcountMethods();
    }

    public void setGradeCodesForDistricts() {
        gradeCodes = SchoolGradeCodes.getAllSchoolGrades();
    }

    public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
        UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
        Map<String, List<UUID>> youthPRPSchoolUUIDs = getPRPAndYouthSchoolUUIDs(previousCollectionID);
        List<UUID> youthPRPSchoolIDs = youthPRPSchoolUUIDs.get("ALLPRPORYOUTH");
        List<UUID> youthSchoolIDs = youthPRPSchoolUUIDs.get("YOUTH");
        List<UUID> shortPRPSchoolIDs = youthPRPSchoolUUIDs.get("SHORT_PRP");
        List<UUID> longPRPSchoolIDs = youthPRPSchoolUUIDs.get("LONG_PRP");

        List<PRPorYouthHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getYouthPRPHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(previousCollectionID, youthPRPSchoolIDs,
                youthSchoolIDs, shortPRPSchoolIDs, longPRPSchoolIDs);

        HeadcountResultsTable previousCollectionData = convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
        List<HeadcountHeader> previousHeadcountHeaderList = this.getHeaders(previousCollectionID, youthPRPSchoolIDs, youthSchoolIDs, shortPRPSchoolIDs, longPRPSchoolIDs);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
        setResultsTableComparisonValuesDynamicNested(collectionData, previousCollectionData);
    }

    public HeadcountResultsTable convertHeadcountResultsToSchoolGradeTable(UUID sdcDistrictCollectionID, List<PRPorYouthHeadcountResult> results) throws EntityNotFoundException {
        HeadcountResultsTable table = new HeadcountResultsTable();
        List<String> headers = new ArrayList<>();
        Set<String> grades = new HashSet<>(gradeCodes);
        Map<String, Map<String, Integer>> schoolGradeCounts = new HashMap<>();
        Map<String, Integer> totalCounts = new HashMap<>();
        Map<String, String> schoolDetails  = new HashMap<>();

        Map<String, List<SchoolTombstone>> youthPRPSchoolTombstones = getAllPRPAndYouthSchoolTombstones(sdcDistrictCollectionID);
        List<SchoolTombstone> allSchools =  youthPRPSchoolTombstones.get("ALLPRPORYOUTH");

        // Collect all grades and initialize school-grade map
        for (PRPorYouthHeadcountResult result : results) {
            if (grades.contains(result.getEnrolledGradeCode())) {
                schoolGradeCounts.computeIfAbsent(result.getSchoolID(), k -> new HashMap<>());
                schoolDetails.putIfAbsent(result.getSchoolID(),
                        restUtils.getSchoolBySchoolID(result.getSchoolID())
                                .map(school -> school.getMincode() + " - " + school.getDisplayName())
                                .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", result.getSchoolID())));
            }
        }

        for (SchoolTombstone school : allSchools) {
            schoolGradeCounts.computeIfAbsent(school.getSchoolId(), k -> new HashMap<>());
            schoolDetails.putIfAbsent(school.getSchoolId(), school.getMincode() + " - " + school.getDisplayName());
        }

        // Initialize totals for each grade
        for (String grade : gradeCodes) {
            totalCounts.put(grade, 0);
            schoolGradeCounts.values().forEach(school -> school.putIfAbsent(grade, 0));
        }

        // Sort grades and add to headers
        headers.add(TITLE);
        headers.addAll(gradeCodes);
        headers.add(TOTAL);
        table.setHeaders(headers);

        // Populate counts for each school and grade
        Map<String, Integer> schoolTotals = new HashMap<>();
        for (PRPorYouthHeadcountResult result : results) {
            if (gradeCodes.contains(result.getEnrolledGradeCode())) {
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
        totalRow.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.values().stream().mapToInt(Integer::intValue).sum())).build());
        rows.add(totalRow);

        // Create rows for the table, including school names
        schoolGradeCounts.forEach((schoolID, gradesCount) -> {
            Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();
            rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolDetails.get(schoolID)).build());
            rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(ALL_SCHOOLS).build());
            gradesCount.forEach((grade, count) -> rowData.put(grade, HeadcountHeaderColumn.builder().currentValue(String.valueOf(count)).build()));
            rowData.put(TOTAL, HeadcountHeaderColumn.builder().currentValue(String.valueOf(schoolTotals.getOrDefault(schoolID, 0))).build());
            rows.add(rowData);
        });

        table.setRows(rows);
        return table;
    }

    private int getCountFromResult(PRPorYouthHeadcountResult result) {
        try {
            return Integer.parseInt(result.getYouthPRPTotals());
        } catch (NumberFormatException e) {
            log.error("Error parsing count from result for SchoolID {}: {}", result.getSchoolID(), e.getMessage());
            return 0;
        }
    }

    public List<HeadcountHeader> getHeaders(UUID sdcDistrictCollectionID, List<UUID> youthPRPSchoolIDs, List<UUID> youthSchoolIDs, List<UUID> shortPRPSchoolIDs, List<UUID> longPRPSchoolIDs) {
        PRPorYouthHeadcountResult result = sdcSchoolCollectionStudentRepository.getPRPYouthHeadersByDistrictId(sdcDistrictCollectionID, youthPRPSchoolIDs, youthSchoolIDs, shortPRPSchoolIDs, longPRPSchoolIDs);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        Arrays.asList(YOUTH_TITLE, SHORT_PRP_TITLE, LONG_PRP_TITLE, ALL_TITLE).forEach(headerTitle -> {
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);
            headcountHeader.setOrderedColumnTitles(List.of(REPORTED_TITLE));
            switch (headerTitle) {
                case YOUTH_TITLE -> {
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getYouthTotals()).build());
                }
                case SHORT_PRP_TITLE -> {
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getShortPRPTotals()).build());
                }
                case LONG_PRP_TITLE -> {
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getLongPRPTotals()).build());
                }
                case ALL_TITLE -> {
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getYouthPRPTotals()).build());
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

    private Map<String, Function<PRPorYouthHeadcountResult, String>> getHeadcountMethods() {
        Map<String, Function<PRPorYouthHeadcountResult, String>> headcountMethods = new HashMap<>();

        headcountMethods.put(YOUTH_TITLE, PRPorYouthHeadcountResult::getYouthTotals);
        headcountMethods.put(SHORT_PRP_TITLE, PRPorYouthHeadcountResult::getShortPRPTotals);
        headcountMethods.put(LONG_PRP_TITLE, PRPorYouthHeadcountResult::getLongPRPTotals);
        headcountMethods.put(ALL_TITLE, PRPorYouthHeadcountResult::getYouthPRPTotals);
        return headcountMethods;
    }

    public Map<String, List<SchoolTombstone>> getAllPRPAndYouthSchoolTombstones(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionEntity> allSchoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);

        Map<String, String> facilityTypeCodes = Map.of(
                "YOUTH", FacilityTypeCodes.YOUTH.getCode(),
                "SHORT_PRP", FacilityTypeCodes.SHORT_PRP.getCode(),
                "LONG_PRP", FacilityTypeCodes.LONG_PRP.getCode()
        );

        Map<String, List<SchoolTombstone>> result = new HashMap<>();
        // Get all schools
        List<SchoolTombstone> allSchools = allSchoolCollections.stream()
                .map(schoolCollection -> restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString())
                        .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollection.class, "SchoolID", schoolCollection.getSchoolID().toString())))
                .filter(school -> facilityTypeCodes.containsValue(school.getFacilityTypeCode()))
                .toList();
        result.put("ALLPRPORYOUTH", allSchools);

        // Get by type
        facilityTypeCodes.forEach((key, code) -> {
            List<SchoolTombstone> schools = allSchools.stream()
                    .filter(school -> code.equals(school.getFacilityTypeCode()))
                    .toList();
            result.put(key, schools);
        });

        return result;
    }

    public Map<String, List<UUID>> getPRPAndYouthSchoolUUIDs(UUID sdcDistrictCollectionID) {
        Map<String, List<SchoolTombstone>> schoolTombstoneMap = getAllPRPAndYouthSchoolTombstones(sdcDistrictCollectionID);
        Map<String, List<UUID>> uuidMap = new HashMap<>();
        schoolTombstoneMap.forEach((key, value) ->
                uuidMap.put(key, value.stream().map(SchoolTombstone::getSchoolId).map(UUID::fromString).toList())
        );
        return uuidMap;
    }

}
