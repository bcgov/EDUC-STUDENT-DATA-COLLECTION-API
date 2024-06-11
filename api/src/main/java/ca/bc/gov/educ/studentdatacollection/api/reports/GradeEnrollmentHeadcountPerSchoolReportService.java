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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
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
public class GradeEnrollmentHeadcountPerSchoolReportService extends BaseReportGenerationService<EnrollmentHeadcountResult> {

    private static final String HEADCOUNT = "headcount";
    private static final String TOTALFTE = "totalFTE";
    private static final String ALLSCHOOLS = "allSchools";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private List<EnrollmentHeadcountResult> gradeEnrollmentHeadcountList;
    private JasperReport gradeEnrollmentPerSchoolHeadcountReport;
    private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    public GradeEnrollmentHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
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
            InputStream inputGradeEnrollmentHeadcount = getClass().getResourceAsStream("/reports/gradeEnrollmentHeadcountsPerSchool.jrxml");
            gradeEnrollmentPerSchoolHeadcountReport = JasperCompileManager.compileReport(inputGradeEnrollmentHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generatePerSchoolReport(UUID collectionID){
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection by Id", collectionID.toString()));

            var programList = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.gradeEnrollmentHeadcountList = programList;
            log.debug(convertToGradeEnrollmentProgramReportJSONStringDistrict(programList, sdcDistrictCollectionEntity));
            return generateJasperReport(convertToGradeEnrollmentProgramReportJSONStringDistrict(programList, sdcDistrictCollectionEntity), gradeEnrollmentPerSchoolHeadcountReport, ReportTypeCode.DIS_GRADE_ENROLLMENT_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for dis grade enrollment programs :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for dis grade enrollment programs :: " + e.getMessage());
        }
    }

    private String convertToGradeEnrollmentProgramReportJSONStringDistrict(List<EnrollmentHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(true);

        mappedResults.forEach(gradeEnrollmentHeadcountResult -> setValueForGrade(nodeMap, gradeEnrollmentHeadcountResult));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }


    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();

        int sequencePrefix = 0;
        if (gradeEnrollmentHeadcountList != null) {
            for (EnrollmentHeadcountResult result : gradeEnrollmentHeadcountList) {
                String schoolID = result.getSchoolID();
                Optional<School> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
                int finalSequencePrefix = sequencePrefix;
                schoolOptional.ifPresent(school -> {
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, "schoolTitle" + schoolID, schoolTitle, finalSequencePrefix == 0 ? "00" : String.valueOf(finalSequencePrefix), includeKH, true, false);
                    addValuesForSectionToMap(nodeMap, HEADCOUNT + schoolID, "Headcount", String.valueOf(finalSequencePrefix + 1), includeKH, false, false);
                    addValuesForSectionToMap(nodeMap, TOTALFTE + schoolID, "FTE Total", String.valueOf(finalSequencePrefix + 2), includeKH, false, true);
                });
                sequencePrefix += 10;
            }
        }
        addValuesForSectionToMap(nodeMap, ALLSCHOOLS, "All Schools", String.valueOf(sequencePrefix), includeKH, true, false);
        addValuesForSectionToMap(nodeMap, HEADCOUNT + ALLSCHOOLS, "Headcount", String.valueOf(sequencePrefix + 1), includeKH, false, false);
        addValuesForSectionToMap(nodeMap, TOTALFTE + ALLSCHOOLS, "FTE Total", String.valueOf(sequencePrefix + 2), includeKH, false, true);
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH, boolean header, boolean isDoubleRow){
        if (Boolean.TRUE.equals(header)) {
            nodeMap.put(sectionPrefix, new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", isDoubleRow, true, true, includeKH));
        } else {
            nodeMap.put(sectionPrefix, new HeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", isDoubleRow, true, true, includeKH));
        }
    }

    protected void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, EnrollmentHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

        HeadcountChildNode allSchoolsHeadcountNode = nodeMap.get("headcountallSchools");
        HeadcountChildNode allSchoolsFTENode = nodeMap.get("totalFTEallSchools");
        if (allSchoolsHeadcountNode.getValueForGrade(code) == null) {
            allSchoolsHeadcountNode.setValueForGrade(code, "0");
        }
        if (allSchoolsFTENode.getValueForGrade(code) == null) {
            allSchoolsFTENode.setValueForGrade(code, "0.0");
        }

        String schoolID = gradeResult.getSchoolID();
        if (nodeMap.containsKey(HEADCOUNT + schoolID)) {
            nodeMap.get(HEADCOUNT + schoolID).setValueForGrade(code, gradeResult.getTotalHeadcount());
        } else if (nodeMap.containsKey(TOTALFTE + schoolID)) {
            nodeMap.get(TOTALFTE + schoolID).setValueForGrade(code, gradeResult.getTotalFteTotal());
        } else {
            log.warn("School ID {} not found in node map", schoolID);
        }

        int currentHeadcountTotal = Integer.parseInt(gradeResult.getTotalHeadcount());
        float currentFTETotal = Float.parseFloat(gradeResult.getTotalFteTotal());
        int accumulatedHeadcountTotal = Integer.parseInt(allSchoolsHeadcountNode.getValueForGrade(code));
        float accumulatedFTETotal = Float.parseFloat(allSchoolsFTENode.getValueForGrade(code));
        allSchoolsHeadcountNode.setValueForGrade(code, String.valueOf(accumulatedHeadcountTotal + currentHeadcountTotal));
        allSchoolsFTENode.setValueForGrade(code, String.valueOf(accumulatedFTETotal + currentFTETotal));
    }
}
