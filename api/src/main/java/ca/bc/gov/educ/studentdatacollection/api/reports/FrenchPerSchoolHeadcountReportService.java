package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchCombinedHeadcountResult;
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
public class FrenchPerSchoolHeadcountReportService extends BaseReportGenerationService<FrenchCombinedHeadcountResult> {

    protected static final String ALLFRENCH = "allFrench";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final RestUtils restUtils;
    private List<SchoolTombstone> allSchoolsTombstones;
    private List<FrenchCombinedHeadcountResult> frenchCombinedHeadcountList;
    private JasperReport frenchPerSchoolHeadcountReport;

    public FrenchPerSchoolHeadcountReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository , RestUtils restUtils) {
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
            InputStream inputFrenchProgramHeadcount = getClass().getResourceAsStream("/reports/frenchProgramHeadcountsPerSchool.jrxml");
            frenchPerSchoolHeadcountReport = JasperCompileManager.compileReport(inputFrenchProgramHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generatePerSchoolReport(UUID sdcDistrictCollectionID){
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            this.allSchoolsTombstones = getAllSchoolTombstones(sdcDistrictCollectionID);
            var programList = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.frenchCombinedHeadcountList = programList;
            return generateJasperReport(convertToFrenchProgramReportJSONStringDistrict(programList, sdcDistrictCollectionEntity), frenchPerSchoolHeadcountReport, DistrictReportTypeCode.DIS_FRENCH_HEADCOUNT_PER_SCHOOL.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
        }
    }

    private String convertToFrenchProgramReportJSONStringDistrict(List<FrenchCombinedHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(false);

        mappedResults.forEach(combinedFrenchHeadcountResult -> setRowValues(nodeMap, combinedFrenchHeadcountResult));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLFRENCH, "All French Programs", "00", includeKH);
        int sequencePrefix = 10;

        if (frenchCombinedHeadcountList != null) {
            for (FrenchCombinedHeadcountResult result : frenchCombinedHeadcountList) {
                String schoolID = result.getSchoolID();
                Optional<SchoolTombstone> schoolOptional = restUtils.getSchoolBySchoolID(schoolID);
                int finalSequencePrefix = sequencePrefix;
                schoolOptional.ifPresent(school -> {
                    includedSchoolIDs.add(school.getSchoolId());
                    String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                    addValuesForSectionToMap(nodeMap, schoolID, schoolTitle, String.valueOf(finalSequencePrefix), includeKH);
                });
                sequencePrefix += 10;
            }
        }

        for (SchoolTombstone school : allSchoolsTombstones) {
            if (!includedSchoolIDs.contains(school.getSchoolId())) {
                String schoolTitle = school.getMincode() + " - " + school.getDisplayName();
                addValuesForSectionToMap(nodeMap, school.getSchoolId(), schoolTitle, String.valueOf(sequencePrefix), includeKH);
                sequencePrefix += 10;
            }
        }

        return nodeMap;
    }

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH){
        if (Objects.equals(sectionPrefix, ALLFRENCH)) {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, false, includeKH));
        } else {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", false, true, false, includeKH));
        }
    }

    protected void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, FrenchCombinedHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

        GradeHeadcountChildNode allFrenchNode = (GradeHeadcountChildNode)nodeMap.get(ALLFRENCH);
        if (allFrenchNode.getValueForGrade(code) == null) {
            allFrenchNode.setValueForGrade(code, "0");
        }

        String schoolID = gradeResult.getSchoolID();
        if (nodeMap.containsKey(schoolID)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID)).setValueForGrade(code, gradeResult.getTotalTotals());
        } else {
            log.warn("School ID {} not found in node map", schoolID);
        }

        int currentTotal = Integer.parseInt(gradeResult.getTotalTotals());
        int accumulatedTotal = Integer.parseInt(allFrenchNode.getValueForGrade(code));
        allFrenchNode.setValueForGrade(code, String.valueOf(accumulatedTotal + currentTotal));
    }
}
