package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchCombinedHeadcountResult;
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
public class FrenchPerSchoolHeadcountReportService extends BaseReportGenerationService<FrenchCombinedHeadcountResult> {

    private static final String HEADING = "Heading";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private List<FrenchCombinedHeadcountResult> frenchCombinedHeadcountList;
    private JasperReport frenchPerSchoolHeadcountReport;
    private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    public FrenchPerSchoolHeadcountReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
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
            InputStream inputFrenchProgramHeadcount = getClass().getResourceAsStream("/reports/frenchProgramHeadcountsPerSchool.jrxml");
            frenchPerSchoolHeadcountReport = JasperCompileManager.compileReport(inputFrenchProgramHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generatePerSchoolReport(UUID collectionID){
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection by Id", collectionID.toString()));

            var programList = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.frenchCombinedHeadcountList = programList;
            return generateJasperReport(convertToFrenchProgramReportJSONStringDistrict(programList, sdcDistrictCollectionEntity), frenchPerSchoolHeadcountReport, ReportTypeCode.DIS_FRENCH_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
        }
    }

    private String convertToFrenchProgramReportJSONStringDistrict(List<FrenchCombinedHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(true);

        mappedResults.forEach(combinedFrenchHeadcountResult -> setValueForGrade(nodeMap, combinedFrenchHeadcountResult));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        addValuesForSectionToMap(nodeMap, "allFrench", "All French Programs", "00", includeKH);

        int sequencePrefix = 0;
        if (frenchCombinedHeadcountList != null) {
            for (FrenchCombinedHeadcountResult result : frenchCombinedHeadcountList) {
                String schoolID = result.getSchoolID();
                Optional<School> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
                int finalSequencePrefix = sequencePrefix;
                schoolOptional.ifPresent(school -> {
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, schoolID, schoolTitle, finalSequencePrefix == 0 ? "00" : String.valueOf(finalSequencePrefix), includeKH);
                });
                sequencePrefix += 10;
            }
        }
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH){
        if (Objects.equals(sectionPrefix, "allFrench")) {
            nodeMap.put(sectionPrefix + HEADING, new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, false, includeKH));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new HeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", false, true, false, includeKH));
        }
    }

    protected void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, FrenchCombinedHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));
        // TODO need to deal with total totals
        if (frenchCombinedHeadcountList != null) {
            for (FrenchCombinedHeadcountResult result : frenchCombinedHeadcountList) {
                String schoolID = result.getSchoolID();
                nodeMap.get(schoolID + HEADING).setValueForGrade(code , result.getTotalTotals());
            }
        }
    }
}
