package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.RefugeeHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountReportNode;
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
public class RefugeeHeadcountPerSchoolReportService extends BaseReportGenerationService<RefugeeHeadcountResult> {

    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private JasperReport refugeeHeadcountReport;
    private List<RefugeeHeadcountResult> refugeeHeadcounts;
    private final ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
    private static final String HEADING = "Heading";
    private static final String HEADCOUNT = "Headcount";
    private static final String ALL_REFUGEE_HEADING = "allRefugeeHeading";

    protected RefugeeHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        super(restUtils);
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.restUtils = restUtils;
    }

    @PostConstruct
    public void init() {
        ApplicationProperties.bgTask.execute(this::initialize);
    }
    private void initialize() {
        this.compileJasperReports();
    }
    private void compileJasperReports(){
        try {
            InputStream inputHeadcount = getClass().getResourceAsStream("/reports/refugeeHeadcountsPerSchool.jrxml");
            refugeeHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateRefugeePerSchoolReport(UUID collectionID){
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional =  sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "CollectionId", collectionID.toString()));

            refugeeHeadcounts = sdcSchoolCollectionStudentRepository.getRefugeeHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            return generateJasperReport(convertToRefugeeReportJSONStringDistrict(refugeeHeadcounts, sdcDistrictCollectionEntity), refugeeHeadcountReport, ReportTypeCode.DIS_REFUGEE_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
        }
    }

    private String convertToRefugeeReportJSONStringDistrict(List<RefugeeHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(false);

        mappedResults.forEach(result -> setValueForGrade(nodeMap, result));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        addValuesForSectionToMap(nodeMap, "allRefugee", "All Newcomer Refugees", "00");

        int sequencePrefix = 10;
        if (!refugeeHeadcounts.isEmpty()) {
            for (RefugeeHeadcountResult result : refugeeHeadcounts) {
                String schoolID = result.getSchoolID();
                SchoolTombstone school = restUtils.getSchoolBySchoolID(schoolID).orElse(null);

                if (school != null) {
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, schoolID, schoolTitle, String.valueOf(sequencePrefix));
                }

                sequencePrefix += 10;
            }
        }
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
        if (Objects.equals(sectionPrefix, "allSchools")) {
            nodeMap.put(sectionPrefix + HEADING, new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new HeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", false));
        }
    }

    protected void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, RefugeeHeadcountResult gradeResult) {
        int runningTotalHeadcount = 0;
        double runningTotalFTE = 0.0000;
        int runningTotalELL = 0;

        if (refugeeHeadcounts != null) {
            for (RefugeeHeadcountResult each : refugeeHeadcounts) {
                String schoolKey = each.getSchoolID();
                runningTotalHeadcount += Integer.parseInt(each.getHeadcount());
                runningTotalFTE += Double.parseDouble(each.getFteTotal());
                runningTotalELL += Integer.parseInt(each.getEll());
                nodeMap.get(schoolKey + HEADING).setValueForBand(HEADCOUNT, each.getHeadcount());
                nodeMap.get(schoolKey + HEADING).setValueForBand("FTE", each.getFteTotal());
                nodeMap.get(schoolKey + HEADING).setValueForBand("ELL", each.getEll());
            }
        }

        nodeMap.get(ALL_REFUGEE_HEADING).setValueForBand(HEADCOUNT, String.valueOf(runningTotalHeadcount));
        nodeMap.get(ALL_REFUGEE_HEADING).setValueForBand("FTE", String.format("%.4f", runningTotalFTE));
        nodeMap.get(ALL_REFUGEE_HEADING).setValueForBand(HEADCOUNT, String.valueOf(runningTotalELL));
    }
}
