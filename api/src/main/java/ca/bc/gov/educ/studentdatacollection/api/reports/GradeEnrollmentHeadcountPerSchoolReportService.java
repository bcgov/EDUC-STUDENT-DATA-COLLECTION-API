package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
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
public class GradeEnrollmentHeadcountPerSchoolReportService extends BaseReportGenerationService<EnrollmentHeadcountResult> {

    private static final String HEADCOUNT = "headcount";
    private static final String HEADCOUNTTITLE = "Headcount";
    private static final String FTETOTALTITLE = "FTE Total";
    private static final String TOTALFTE = "totalFTE";
    private static final String ALLSCHOOLS = "allSchools";
    private static final String SCHOOLTITLE = "schoolTitle";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private List<EnrollmentHeadcountResult> gradeEnrollmentHeadcountList;
    private List<SchoolTombstone> allSchoolsTombstones;
    private JasperReport gradeEnrollmentPerSchoolHeadcountReport;
    private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

    public GradeEnrollmentHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository , RestUtils restUtils) {
        super(restUtils, sdcSchoolCollectionRepository);
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

            this.allSchoolsTombstones = getAllSchoolTombstones(collectionID);
            var programList = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.gradeEnrollmentHeadcountList = programList;
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

        mappedResults.forEach(gradeEnrollmentHeadcountResult -> setRowValues(nodeMap, gradeEnrollmentHeadcountResult));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }


    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();

        int sequencePrefix = 10;
        if (gradeEnrollmentHeadcountList != null) {
            for (EnrollmentHeadcountResult result : gradeEnrollmentHeadcountList) {
                String schoolID = result.getSchoolID();
                Optional<SchoolTombstone> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
                int finalSequencePrefix = sequencePrefix;
                schoolOptional.ifPresent(school -> {
                    includedSchoolIDs.add(school.getSchoolId());
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, SCHOOLTITLE + schoolID, schoolTitle, String.valueOf(finalSequencePrefix), includeKH, true, false);
                    addValuesForSectionToMap(nodeMap, HEADCOUNT + schoolID, HEADCOUNTTITLE, String.valueOf(finalSequencePrefix + 1), includeKH, false, false);
                    addValuesForSectionToMap(nodeMap, TOTALFTE + schoolID, FTETOTALTITLE, String.valueOf(finalSequencePrefix + 2), includeKH, false, true);
                });
                sequencePrefix += 10;
            }
        }

        for (SchoolTombstone school : allSchoolsTombstones) {
            if (!includedSchoolIDs.contains(school.getSchoolId())) {
                String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                addValuesForSectionToMap(nodeMap, SCHOOLTITLE + school.getSchoolId(), schoolTitle, String.valueOf(sequencePrefix), includeKH, true, false);
                addValuesForSectionToMap(nodeMap, HEADCOUNT + school.getSchoolId(), HEADCOUNTTITLE, String.valueOf(sequencePrefix + 1), includeKH, false, false);
                addValuesForSectionToMap(nodeMap, TOTALFTE + school.getSchoolId(), FTETOTALTITLE, String.valueOf(sequencePrefix + 2), includeKH, false, true);
                sequencePrefix += 10;
            }
        }

        addValuesForSectionToMap(nodeMap, ALLSCHOOLS, "All Schools", String.valueOf(sequencePrefix), includeKH, true, false);
        addValuesForSectionToMap(nodeMap, HEADCOUNT + ALLSCHOOLS, HEADCOUNTTITLE, String.valueOf(sequencePrefix + 1), includeKH, false, false);
        addValuesForSectionToMap(nodeMap, TOTALFTE + ALLSCHOOLS, FTETOTALTITLE, String.valueOf(sequencePrefix + 2), includeKH, false, true);
        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH, boolean header, boolean isDoubleRow){
        GradeHeadcountChildNode node;
        if (Boolean.TRUE.equals(header)) {
            node = new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", isDoubleRow, true, true, includeKH);
        } else {
            node = new GradeHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", isDoubleRow, true, true, includeKH);
        }

        if (sectionPrefix.startsWith(SCHOOLTITLE) || sectionPrefix.startsWith(ALLSCHOOLS)) {
            node.setAllValuesToNull();
        }

        nodeMap.put(sectionPrefix, node);
    }

    protected void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, EnrollmentHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

        GradeHeadcountChildNode allSchoolsHeadcountNode = (GradeHeadcountChildNode)nodeMap.get("headcountallSchools");
        GradeHeadcountChildNode allSchoolsFTENode = (GradeHeadcountChildNode)nodeMap.get("totalFTEallSchools");

        String schoolID = gradeResult.getSchoolID();

        try {
            if (nodeMap.containsKey(HEADCOUNT + schoolID)) {
                ((GradeHeadcountChildNode)nodeMap.get(HEADCOUNT + schoolID)).setValueForGrade(code, gradeResult.getTotalHeadcount());
            }

            if (nodeMap.containsKey(TOTALFTE + schoolID) && gradeResult.getTotalFteTotal() != null) {
                double fteTotal = numberFormat.parse(gradeResult.getTotalFteTotal()).doubleValue();
                ((GradeHeadcountChildNode)nodeMap.get(TOTALFTE + schoolID)).setValueForGrade(code, String.format("%.4f", fteTotal));
            }

            int currentHeadcountTotal = Integer.parseInt(gradeResult.getTotalHeadcount().replace(",", ""));
            double currentFTETotal = numberFormat.parse(gradeResult.getTotalFteTotal()).doubleValue();

            int accumulatedHeadcountTotal = Integer.parseInt(allSchoolsHeadcountNode.getValueForGrade(code).replace(",", ""));
            double accumulatedFTETotal = numberFormat.parse(allSchoolsFTENode.getValueForGrade(code)).doubleValue();

            allSchoolsHeadcountNode.setValueForGrade(code, String.valueOf(accumulatedHeadcountTotal + currentHeadcountTotal));
            allSchoolsFTENode.setValueForGrade(code, String.format("%.4f", accumulatedFTETotal + currentFTETotal));
        } catch (ParseException e) {
            log.error("Exception occurred while writing PDF report for grade enrollment dis per school - parse error :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment dis per school - parse error:: " + e.getMessage());
        }
    }
}
