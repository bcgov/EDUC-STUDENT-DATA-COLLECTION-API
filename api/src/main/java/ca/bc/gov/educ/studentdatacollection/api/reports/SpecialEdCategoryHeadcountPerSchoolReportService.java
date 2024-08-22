package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SpecialEdHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

@Service
@Slf4j
public class SpecialEdCategoryHeadcountPerSchoolReportService extends BaseReportGenerationService<SpecialEdHeadcountResult> {

    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport inclusiveEdCategoryHeadcountPerSchoolReport;
    private final RestUtils restUtils;
    private List<SpecialEdHeadcountResult> inclEdHeadcounts = new ArrayList<>();
    private List<SchoolTombstone> allSchoolsTombstones;

    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    private static final String ALL_SCHOOLS = "allSchools";

    public SpecialEdCategoryHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, RestUtils restUtils1) {
        super(restUtils, sdcSchoolCollectionRepository);

        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils1;
    }

    @PostConstruct
    public void init() {
        ApplicationProperties.bgTask.execute(this::initialize);
    }

    private void initialize() {
        this.compileJasperReports();
    }

    private void compileJasperReports() {
        try {
            InputStream inputSpecialEdHeadcount = getClass().getResourceAsStream("/reports/specialEdProgramCategoryHeadcountsPerSchool.jrxml");
            inclusiveEdCategoryHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputSpecialEdHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateInclusiveEdCategoryHeadcountPerSchoolReport(UUID collectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "CollectionId", collectionID.toString()));

            inclEdHeadcounts = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryBySchoolIdAndSdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(collectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(inclEdHeadcounts, sdcDistrictCollectionEntity), inclusiveEdCategoryHeadcountPerSchoolReport, ReportTypeCode.DIS_SPECIAL_EDUCATION_CATEGORY_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
        }
    }

    @Override
    protected String convertToReportJSONStringDistrict(List<SpecialEdHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(false);

        mappedResults.forEach(result -> setRowValues(nodeMap, result));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }


    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALL_SCHOOLS, "All Schools","00");

        int sequencePrefix = 10;
        if (!inclEdHeadcounts.isEmpty()) {
            for (SpecialEdHeadcountResult result : inclEdHeadcounts) {
                String schoolID = result.getSchoolID();
                Optional<SchoolTombstone> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
                int finalSequencePrefix = sequencePrefix;
                schoolOptional.ifPresent(school -> {
                    includedSchoolIDs.add(school.getSchoolId());
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, schoolID, schoolTitle, String.valueOf(finalSequencePrefix));
                });
                sequencePrefix += 10;
            }
        }

        for (SchoolTombstone school : allSchoolsTombstones) {
            if (!includedSchoolIDs.contains(school.getSchoolId())) {
                String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                addValuesForSectionToMap(nodeMap, school.getSchoolId(), schoolTitle, String.valueOf(sequencePrefix));
                sequencePrefix += 10;
            }
        }

        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
        if (Objects.equals(sectionPrefix, ALL_SCHOOLS)) {
            nodeMap.put(sectionPrefix, new SpedCategoryHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0"));
        } else {
            nodeMap.put(sectionPrefix, new SpedCategoryHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0"));
        }
    }

    protected void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, SpecialEdHeadcountResult result) {
        int runningTotalAHeadcount = 0;
        int runningTotalBHeadcount = 0;
        int runningTotalCHeadcount = 0;
        int runningTotalDHeadcount = 0;
        int runningTotalEHeadcount = 0;
        int runningTotalFHeadcount = 0;
        int runningTotalGHeadcount = 0;
        int runningTotalHHeadcount = 0;
        int runningTotalKHeadcount = 0;
        int runningTotalPHeadcount = 0;
        int runningTotalQHeadcount = 0;
        int runningTotalRHeadcount = 0;

        if (inclEdHeadcounts != null) {
            for (SpecialEdHeadcountResult headcount : inclEdHeadcounts) {
                try {
                    String schoolKey = headcount.getSchoolID();

                    SpedCategoryHeadcountChildNode node = (SpedCategoryHeadcountChildNode)nodeMap.getOrDefault(schoolKey, new SpedCategoryHeadcountChildNode()); 
                    
                    String inclEdAHeadcount = headcount.getAdultsInSpecialEdA() ? headcount.getSpecialEdACodes() + "*" : headcount.getSpecialEdACodes();
                    node.setValueForSpecEdCategory("A", inclEdAHeadcount);
                    runningTotalAHeadcount += numberFormat.parse(inclEdAHeadcount).intValue();

                    String inclEdBHeadcount = headcount.getAdultsInSpecialEdB() ? headcount.getSpecialEdBCodes() + "*" : headcount.getSpecialEdBCodes();
                    node.setValueForSpecEdCategory("B", inclEdBHeadcount);
                    runningTotalBHeadcount += numberFormat.parse(inclEdBHeadcount).intValue();

                    String inclEdCHeadcount = headcount.getAdultsInSpecialEdC() ? headcount.getSpecialEdCCodes() + "*" : headcount.getSpecialEdCCodes();
                    node.setValueForSpecEdCategory("C", inclEdCHeadcount);
                    runningTotalCHeadcount += numberFormat.parse(inclEdCHeadcount).intValue();

                    String inclEdDHeadcount = headcount.getAdultsInSpecialEdD() ? headcount.getSpecialEdDCodes() + "*" : headcount.getSpecialEdDCodes();
                    node.setValueForSpecEdCategory("D", inclEdDHeadcount);
                    runningTotalDHeadcount += numberFormat.parse(inclEdDHeadcount).intValue();

                    String inclEdEHeadcount = headcount.getAdultsInSpecialEdE() ? headcount.getSpecialEdECodes() + "*" : headcount.getSpecialEdECodes();
                    node.setValueForSpecEdCategory("E", inclEdEHeadcount);
                    runningTotalEHeadcount += numberFormat.parse(inclEdEHeadcount).intValue();

                    String inclEdFHeadcount = headcount.getAdultsInSpecialEdF() ? headcount.getSpecialEdFCodes() + "*" : headcount.getSpecialEdFCodes();
                    node.setValueForSpecEdCategory("F", inclEdFHeadcount);
                    runningTotalFHeadcount += numberFormat.parse(inclEdFHeadcount).intValue();

                    String inclEdGHeadcount = headcount.getAdultsInSpecialEdG() ? headcount.getSpecialEdGCodes() + "*" : headcount.getSpecialEdGCodes();
                    node.setValueForSpecEdCategory("G", inclEdGHeadcount);
                    runningTotalGHeadcount += numberFormat.parse(inclEdGHeadcount).intValue();

                    String inclEdHHeadcount = headcount.getAdultsInSpecialEdH() ? headcount.getSpecialEdHCodes() + "*" : headcount.getSpecialEdHCodes();
                    node.setValueForSpecEdCategory("H", inclEdHHeadcount);
                    runningTotalHHeadcount += numberFormat.parse(inclEdHHeadcount).intValue();

                    String inclEdKHeadcount = headcount.getAdultsInSpecialEdK() ? headcount.getSpecialEdKCodes() + "*" : headcount.getSpecialEdKCodes();
                    node.setValueForSpecEdCategory("K", inclEdKHeadcount);
                    runningTotalKHeadcount += numberFormat.parse(inclEdKHeadcount).intValue();

                    String inclEdPHeadcount = headcount.getAdultsInSpecialEdP() ? headcount.getSpecialEdPCodes() + "*" : headcount.getSpecialEdPCodes();
                    node.setValueForSpecEdCategory("P", inclEdPHeadcount);
                    runningTotalPHeadcount += numberFormat.parse(inclEdPHeadcount).intValue();

                    String inclEdQHeadcount = headcount.getAdultsInSpecialEdQ() ? headcount.getSpecialEdQCodes() + "*" : headcount.getSpecialEdQCodes();
                    node.setValueForSpecEdCategory("Q", inclEdQHeadcount);
                    runningTotalQHeadcount += numberFormat.parse(inclEdQHeadcount).intValue();

                    String inclEdRHeadcount = headcount.getAdultsInSpecialEdR() ? headcount.getSpecialEdRCodes() + "*" : headcount.getSpecialEdRCodes();
                    node.setValueForSpecEdCategory("R", inclEdRHeadcount);
                    runningTotalRHeadcount += numberFormat.parse(inclEdRHeadcount).intValue();

                    String totalHeadcount = headcount.getAllLevels();
                    node.setValueForSpecEdCategory("total", totalHeadcount);

                    nodeMap.put(schoolKey, node);

                } catch (ParseException e) {
                    log.error("Error parsing number in setValueForGrade - Inclusive Education Category Headcount Report: " + e.getMessage());
                    throw new StudentDataCollectionAPIRuntimeException("Error parsing number in setValueForGrade - Inclusive Education Category Headcount Report: " + e.getMessage());
                }
            }
        }

        SpedCategoryHeadcountChildNode totalNode = (SpedCategoryHeadcountChildNode) nodeMap.getOrDefault(ALL_SCHOOLS, new SpedCategoryHeadcountChildNode());
        totalNode.setValueForSpecEdCategory("A", String.valueOf(runningTotalAHeadcount));
        totalNode.setValueForSpecEdCategory("B", String.valueOf(runningTotalBHeadcount));
        totalNode.setValueForSpecEdCategory("C", String.valueOf(runningTotalCHeadcount));
        totalNode.setValueForSpecEdCategory("D", String.valueOf(runningTotalDHeadcount));
        totalNode.setValueForSpecEdCategory("E", String.valueOf(runningTotalEHeadcount));
        totalNode.setValueForSpecEdCategory("F", String.valueOf(runningTotalFHeadcount));
        totalNode.setValueForSpecEdCategory("G", String.valueOf(runningTotalGHeadcount));
        totalNode.setValueForSpecEdCategory("H", String.valueOf(runningTotalHHeadcount));
        totalNode.setValueForSpecEdCategory("K", String.valueOf(runningTotalKHeadcount));
        totalNode.setValueForSpecEdCategory("P", String.valueOf(runningTotalPHeadcount));
        totalNode.setValueForSpecEdCategory("Q", String.valueOf(runningTotalQHeadcount));
        totalNode.setValueForSpecEdCategory("R", String.valueOf(runningTotalRHeadcount));
        Integer allSchoolsTotal = runningTotalAHeadcount + runningTotalBHeadcount + runningTotalCHeadcount + runningTotalDHeadcount + runningTotalEHeadcount + runningTotalFHeadcount + runningTotalGHeadcount + runningTotalHHeadcount + runningTotalKHeadcount + runningTotalPHeadcount + runningTotalQHeadcount + runningTotalRHeadcount;
        totalNode.setValueForSpecEdCategory("total", String.valueOf(allSchoolsTotal));

        nodeMap.put(ALL_SCHOOLS, totalNode);
    }

}
