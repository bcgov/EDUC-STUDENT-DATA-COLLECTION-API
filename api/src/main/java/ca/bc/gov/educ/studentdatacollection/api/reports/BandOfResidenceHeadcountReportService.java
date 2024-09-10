package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.BandResidenceHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.BandHeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import com.fasterxml.jackson.core.JsonProcessingException;
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
public class BandOfResidenceHeadcountReportService extends BaseReportGenerationService<BandResidenceHeadcountResult>{

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport bandOfResidenceHeadcountReport;
    private List<BandResidenceHeadcountResult> headcountsList;
    private final CodeTableService codeTableService;
    private static final String HEADING = "Heading";

    public BandOfResidenceHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, CodeTableService codeTableService) {
        super(restUtils, sdcSchoolCollectionRepository);
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.codeTableService = codeTableService;
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
            InputStream inputHeadcount = getClass().getResourceAsStream("/reports/bandOfResidenceHeadcounts.jrxml");
            bandOfResidenceHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateBandOfResidenceReport(UUID collectionID){
        try {
            Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
            SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionID", collectionID.toString()));

            this.headcountsList = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
            return generateJasperReport(convertToReportJSONString(headcountsList, sdcSchoolCollectionEntity), bandOfResidenceHeadcountReport, SchoolReportTypeCode.BAND_RESIDENCE_HEADCOUNT.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
        }
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        List<BandCodeEntity> allActiveBandCodes = codeTableService.getAllBandCodes();
        int sequencePrefix = 0;
        if (headcountsList != null) {
            for (BandResidenceHeadcountResult result : headcountsList) {
                String bandKey = result.getBandCode();
                Optional<BandCodeEntity> entity = allActiveBandCodes.stream().filter(band -> band.getBandCode().equalsIgnoreCase(bandKey)).findFirst();
                String bandTitle = entity.map(bandCodeEntity -> bandKey + " - " + bandCodeEntity.getLabel()).orElse(bandKey);
                addValuesForSectionToMap(nodeMap, bandKey, bandTitle, sequencePrefix == 0 ? "00" : String.valueOf(sequencePrefix));
                sequencePrefix += 10;
            }
        }
        addValuesForSectionToMap(nodeMap, "allBands", "All Bands & Students", sequencePrefix == 0 ? "00" : String.valueOf(sequencePrefix));
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
        if (Objects.equals(sectionPrefix, "allBands")) {
            nodeMap.put(sectionPrefix + HEADING, new BandHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0"));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new BandHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0"));
        }
    }

    protected void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, BandResidenceHeadcountResult result) {
        double runningTotalFTE = 0.0;
        int runningTotalHeadcount = 0;
        if (headcountsList != null) {
            for (BandResidenceHeadcountResult each : headcountsList) {
                try {
                    String bandKey = each.getBandCode();
                    double fteTotal = numberFormat.parse(each.getFteTotal()).doubleValue();
                    int headcountTotal = numberFormat.parse(each.getHeadcount()).intValue();

                    runningTotalFTE += fteTotal;
                    runningTotalHeadcount += headcountTotal;

                    ((BandHeadcountChildNode)nodeMap.get(bandKey + HEADING)).setValueForBand("FTE", String.format("%.4f", fteTotal));
                    ((BandHeadcountChildNode)nodeMap.get(bandKey + HEADING)).setValueForBand("Headcount", String.valueOf(headcountTotal));
                } catch (ParseException e) {
                    log.error("Error parsing number in setValueForGrade - Band of Residence Report: " + e.getMessage());
                    throw new StudentDataCollectionAPIRuntimeException("Error parsing number in setValueForGrade - Band of Residence Report: " + e.getMessage());
                }
            }
        }
        ((BandHeadcountChildNode)nodeMap.get("allBandsHeading")).setValueForBand("FTE", String.format("%.4f", runningTotalFTE));
        ((BandHeadcountChildNode)nodeMap.get("allBandsHeading")).setValueForBand("Headcount", String.valueOf(runningTotalHeadcount));
    }
}
