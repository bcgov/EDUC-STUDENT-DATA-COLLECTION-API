package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.BandResidenceHeadcountResult;
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
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class BandOfResidenceHeadcountReportService extends BaseReportGenerationService<BandResidenceHeadcountResult>{

    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport bandOfResidenceHeadcountReport;

    public BandOfResidenceHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
        super(restUtils);
        this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
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

    public DownloadableReportResponse generateBandOfResdienceReport(UUID collectionID){
        try {
            Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
            SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

            var headcountsList = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
            return generateJasperReport(convertToReportJSONString(headcountsList, sdcSchoolCollectionEntity), bandOfResidenceHeadcountReport, ReportTypeCode.BAND_RESIDENCE_HEADCOUNT);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for band of residence report :: " + e.getMessage());
        }
    }

    @Override
    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        // TODO populate with "bandName - bandCode"
        // addValuesForSectionToMap(nodeMap, "band x", "band x", "00");
        addValuesForSectionToMap(nodeMap, "allBands", "All Bands & Students", "00");
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
        nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
    }

    @Override
    protected void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, BandResidenceHeadcountResult result) {
        // TODO set the values for headcount and FTE
        nodeMap.get("allBands").setValueForBand("FTE", result.getFteTotal());
        nodeMap.get("allBands").setValueForBand("Headcount", result.getHeadcount());
    }
}
