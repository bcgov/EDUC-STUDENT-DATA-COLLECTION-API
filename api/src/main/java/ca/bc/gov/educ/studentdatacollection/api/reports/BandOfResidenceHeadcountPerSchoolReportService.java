package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
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
import java.util.*;

@Service
@Slf4j
public class BandOfResidenceHeadcountPerSchoolReportService extends BaseReportGenerationService<BandResidenceHeadcountResult>{

    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final RestUtils restUtils;
    private JasperReport bandOfResidenceHeadcountPerSchoolReport;
    private List<SchoolTombstone> allSchoolsTombstones;
    private List<BandResidenceHeadcountResult> bandHeadcounts = new ArrayList<>();
    private static final String HEADING = "Heading";
    protected static final String ALLBAND = "allBand";


    public BandOfResidenceHeadcountPerSchoolReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
        super(restUtils, sdcSchoolCollectionRepository);
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
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
            InputStream inputHeadcount = getClass().getResourceAsStream("/reports/bandOfResidenceHeadcountsPerSchool.jrxml");
            bandOfResidenceHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateBandOfResidenceHeadcountPerSchoolReport(UUID sdcDistrictCollectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            bandHeadcounts = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(sdcDistrictCollectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(bandHeadcounts, sdcDistrictCollectionEntity), bandOfResidenceHeadcountPerSchoolReport, DistrictReportTypeCode.DIS_BAND_RESIDENCE_HEADCOUNT_PER_SCHOOL.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
        }
    }

    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLBAND, "All Band of Residence Headcount for All Schools", "00");

        int sequencePrefix = 10;
        if (!bandHeadcounts.isEmpty()) {
            for (BandResidenceHeadcountResult result : bandHeadcounts) {
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
        if (Objects.equals(sectionPrefix, ALLBAND)) {
            nodeMap.put(sectionPrefix, new BandHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0"));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new BandHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0"));
        }
    }

    public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, BandResidenceHeadcountResult result) {
        String schoolID = result.getSchoolID();

        BandHeadcountChildNode allBandNode = (BandHeadcountChildNode) nodeMap.computeIfAbsent(ALLBAND, k -> new BandHeadcountChildNode());
        BandHeadcountChildNode schoolNode = (BandHeadcountChildNode) nodeMap.computeIfAbsent(schoolID + HEADING, k -> new BandHeadcountChildNode());

        String fteTotalStr = result.getFteTotal() != null ? result.getFteTotal().trim() : "0.0";
        String headcountStr = result.getHeadcount() != null ? result.getHeadcount().trim() : "0";

        double schoolFTE = Double.parseDouble(fteTotalStr);
        int schoolHeadcount = Integer.parseInt(headcountStr);

        double currentFTE = schoolNode.getValueFTE() != null ? Double.parseDouble(schoolNode.getValueFTE()) : 0.0;
        int currentHeadcount = schoolNode.getValueHeadcount() != null ? Integer.parseInt(schoolNode.getValueHeadcount()) : 0;

        schoolNode.setValueFTE(String.format("%.4f", currentFTE + schoolFTE));
        schoolNode.setValueHeadcount(String.valueOf(currentHeadcount + schoolHeadcount));

        double totalFTE = Double.parseDouble(allBandNode.getValueFTE() != null ? allBandNode.getValueFTE() : "0.0");
        int totalHeadcount = Integer.parseInt(allBandNode.getValueHeadcount() != null ? allBandNode.getValueHeadcount() : "0");

        allBandNode.setValueFTE(String.format("%.4f", totalFTE + schoolFTE));
        allBandNode.setValueHeadcount(String.valueOf(totalHeadcount + schoolHeadcount));
    }
}
