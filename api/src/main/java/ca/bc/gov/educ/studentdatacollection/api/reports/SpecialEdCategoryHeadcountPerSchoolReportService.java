package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SpecialEducationHeadcountHeader;
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

    public DownloadableReportResponse generateInclusiveEdCategoryHeadcountPerSchoolReport(UUID sdcDistrictCollectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            inclEdHeadcounts = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryBySchoolIdAndSdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(sdcDistrictCollectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(inclEdHeadcounts, sdcDistrictCollectionEntity), inclusiveEdCategoryHeadcountPerSchoolReport, DistrictReportTypeCode.DIS_SPECIAL_EDUCATION_HEADCOUNT_CATEGORY_PER_SCHOOL.getCode());
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
        Map<String, Integer> spedCatCountMap = SpecialEducationHeadcountHeader.generateSpedCatCountMap();

        if (inclEdHeadcounts != null) {
            for (SpecialEdHeadcountResult headcount : inclEdHeadcounts) {
                String schoolKey = headcount.getSchoolID();

                SpedCategoryHeadcountChildNode node = (SpedCategoryHeadcountChildNode)nodeMap.getOrDefault(schoolKey, new SpedCategoryHeadcountChildNode());

                String inclEdAHeadcount = headcount.getAdultsInSpecialEdA() ? headcount.getSpecialEdACodes() + "*" : headcount.getSpecialEdACodes();
                node.setValueForSpecEdCategory("A", inclEdAHeadcount);
                spedCatCountMap.compute("A", (key, oldValue) -> oldValue + 5);

                String inclEdBHeadcount = headcount.getAdultsInSpecialEdB() ? headcount.getSpecialEdBCodes() + "*" : headcount.getSpecialEdBCodes();
                node.setValueForSpecEdCategory("B", inclEdBHeadcount);
                spedCatCountMap.compute("B", (key, oldValue) -> oldValue + 5);

                String inclEdCHeadcount = headcount.getAdultsInSpecialEdC() ? headcount.getSpecialEdCCodes() + "*" : headcount.getSpecialEdCCodes();
                node.setValueForSpecEdCategory("C", inclEdCHeadcount);
                spedCatCountMap.compute("C", (key, oldValue) -> oldValue + 5);

                String inclEdDHeadcount = headcount.getAdultsInSpecialEdD() ? headcount.getSpecialEdDCodes() + "*" : headcount.getSpecialEdDCodes();
                node.setValueForSpecEdCategory("D", inclEdDHeadcount);
                spedCatCountMap.compute("D", (key, oldValue) -> oldValue + 5);

                String inclEdEHeadcount = headcount.getAdultsInSpecialEdE() ? headcount.getSpecialEdECodes() + "*" : headcount.getSpecialEdECodes();
                node.setValueForSpecEdCategory("E", inclEdEHeadcount);
                spedCatCountMap.compute("E", (key, oldValue) -> oldValue + 5);

                String inclEdFHeadcount = headcount.getAdultsInSpecialEdF() ? headcount.getSpecialEdFCodes() + "*" : headcount.getSpecialEdFCodes();
                node.setValueForSpecEdCategory("F", inclEdFHeadcount);
                spedCatCountMap.compute("F", (key, oldValue) -> oldValue + 5);

                String inclEdGHeadcount = headcount.getAdultsInSpecialEdG() ? headcount.getSpecialEdGCodes() + "*" : headcount.getSpecialEdGCodes();
                node.setValueForSpecEdCategory("G", inclEdGHeadcount);
                spedCatCountMap.compute("G", (key, oldValue) -> oldValue + 5);

                String inclEdHHeadcount = headcount.getAdultsInSpecialEdH() ? headcount.getSpecialEdHCodes() + "*" : headcount.getSpecialEdHCodes();
                node.setValueForSpecEdCategory("H", inclEdHHeadcount);
                spedCatCountMap.compute("H", (key, oldValue) -> oldValue + 5);

                String inclEdKHeadcount = headcount.getAdultsInSpecialEdK() ? headcount.getSpecialEdKCodes() + "*" : headcount.getSpecialEdKCodes();
                node.setValueForSpecEdCategory("K", inclEdKHeadcount);
                spedCatCountMap.compute("K", (key, oldValue) -> oldValue + 5);

                String inclEdPHeadcount = headcount.getAdultsInSpecialEdP() ? headcount.getSpecialEdPCodes() + "*" : headcount.getSpecialEdPCodes();
                node.setValueForSpecEdCategory("P", inclEdPHeadcount);
                spedCatCountMap.compute("P", (key, oldValue) -> oldValue + 5);

                String inclEdQHeadcount = headcount.getAdultsInSpecialEdQ() ? headcount.getSpecialEdQCodes() + "*" : headcount.getSpecialEdQCodes();
                node.setValueForSpecEdCategory("Q", inclEdQHeadcount);
                spedCatCountMap.compute("Q", (key, oldValue) -> oldValue + 5);

                String inclEdRHeadcount = headcount.getAdultsInSpecialEdR() ? headcount.getSpecialEdRCodes() + "*" : headcount.getSpecialEdRCodes();
                node.setValueForSpecEdCategory("R", inclEdRHeadcount);
                spedCatCountMap.compute("R", (key, oldValue) -> oldValue + 5);

                String totalHeadcount = headcount.getAllLevels();
                node.setValueForSpecEdCategory("total", totalHeadcount);

                nodeMap.put(schoolKey, node);
            }
        }

        SpedCategoryHeadcountChildNode totalNode = (SpedCategoryHeadcountChildNode) nodeMap.getOrDefault(ALL_SCHOOLS, new SpedCategoryHeadcountChildNode());
        totalNode.setValueForSpecEdCategory("A", String.valueOf(spedCatCountMap.get("A")));
        totalNode.setValueForSpecEdCategory("B", String.valueOf(spedCatCountMap.get("B")));
        totalNode.setValueForSpecEdCategory("C", String.valueOf(spedCatCountMap.get("C")));
        totalNode.setValueForSpecEdCategory("D", String.valueOf(spedCatCountMap.get("D")));
        totalNode.setValueForSpecEdCategory("E", String.valueOf(spedCatCountMap.get("E")));
        totalNode.setValueForSpecEdCategory("F", String.valueOf(spedCatCountMap.get("F")));
        totalNode.setValueForSpecEdCategory("G", String.valueOf(spedCatCountMap.get("G")));
        totalNode.setValueForSpecEdCategory("H", String.valueOf(spedCatCountMap.get("H")));
        totalNode.setValueForSpecEdCategory("K", String.valueOf(spedCatCountMap.get("I")));
        totalNode.setValueForSpecEdCategory("P", String.valueOf(spedCatCountMap.get("P")));
        totalNode.setValueForSpecEdCategory("Q", String.valueOf(spedCatCountMap.get("Q")));
        totalNode.setValueForSpecEdCategory("R", String.valueOf(spedCatCountMap.get("R")));
        Integer allSchoolsTotal = spedCatCountMap.values().stream().mapToInt(Integer::intValue).sum();
        totalNode.setValueForSpecEdCategory("total", String.valueOf(allSchoolsTotal));

        nodeMap.put(ALL_SCHOOLS, totalNode);
    }

}
