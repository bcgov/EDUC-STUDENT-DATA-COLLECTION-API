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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SpecialEdHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.GradeHeadcountChildNode;
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
public class SpecialEdHeadcountPerSchoolReportService extends BaseReportGenerationService<SpecialEdHeadcountResult> {

    protected static final String ALLSPED = "allSped";
    public static final String HEADING = "Heading";
    public static final String LEVEL_1 = "level1";
    public static final String LEVEL_2 = "level2";
    public static final String LEVEL_3 = "level3";
    public static final String OTHER = "other";
    public static final String ALL = "all";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport specialEdHeadcountPerSchoolReport;
    private final RestUtils restUtils;
    private List<SpecialEdHeadcountResult> spedHeadcounts = new ArrayList<>();
    private List<SchoolTombstone> allSchoolsTombstones;

    public SpecialEdHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, RestUtils restUtils1) {
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
            InputStream inputSpecialEdHeadcount = getClass().getResourceAsStream("/reports/specialEdHeadcountsPerSchool.jrxml");
            specialEdHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputSpecialEdHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateSpecialEdHeadcountPerSchoolReport(UUID sdcDistrictCollectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            spedHeadcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(sdcDistrictCollectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(spedHeadcounts, sdcDistrictCollectionEntity), specialEdHeadcountPerSchoolReport, DistrictReportTypeCode.DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
        }
    }

    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLSPED, "All Inclusive Education Headcount for All Schools", "00");

        int sequencePrefix = 10;
        if (!spedHeadcounts.isEmpty()) {
            for (SpecialEdHeadcountResult result : spedHeadcounts) {
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

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix) {
        if (Objects.equals(sectionPrefix, ALLSPED)) {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, false, false, false));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
            nodeMap.put(sectionPrefix + LEVEL_1, new GradeHeadcountChildNode("Level 1", FALSE, sequencePrefix + "1", false));
            nodeMap.put(sectionPrefix + LEVEL_2, new GradeHeadcountChildNode("Level 2", FALSE, sequencePrefix + "2", false));
            nodeMap.put(sectionPrefix + LEVEL_3, new GradeHeadcountChildNode("Level 3", FALSE, sequencePrefix + "3", false));
            nodeMap.put(sectionPrefix + OTHER, new GradeHeadcountChildNode("Other", FALSE, sequencePrefix + "4", false));
            nodeMap.put(sectionPrefix + ALL, new GradeHeadcountChildNode("All Levels & Categories", FALSE, sequencePrefix + "5", false));
        }
    }

    public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, SpecialEdHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));
        String schoolID = gradeResult.getSchoolID();

        GradeHeadcountChildNode allSpedNode = (GradeHeadcountChildNode) nodeMap.get(ALLSPED);
        if (allSpedNode.getValueForGrade(code) == null) {
            allSpedNode.setValueForGrade(code, "0");
        }

        if (nodeMap.containsKey(schoolID + LEVEL_1)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + LEVEL_1)).setValueForGrade(code, gradeResult.getLevelOnes());
        }

        if (nodeMap.containsKey(schoolID + LEVEL_2)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + LEVEL_2)).setValueForGrade(code, gradeResult.getLevelTwos());
        }

        if (nodeMap.containsKey(schoolID + LEVEL_3)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + LEVEL_3)).setValueForGrade(code, gradeResult.getLevelThrees());
        }

        if (nodeMap.containsKey(schoolID + OTHER)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + OTHER)).setValueForGrade(code, gradeResult.getOtherLevels());
        }

        if (nodeMap.containsKey(schoolID + ALL)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + ALL)).setValueForGrade(code, gradeResult.getAllLevels());
        }

        if (nodeMap.containsKey(schoolID + HEADING)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + HEADING)).setAllValuesToNull();
        }

        int currentTotal = Integer.parseInt(gradeResult.getAllLevels());
        int accumulatedTotal = Integer.parseInt(allSpedNode.getValueForGrade(code));
        allSpedNode.setValueForGrade(code, String.valueOf(accumulatedTotal + currentTotal));
    }
}
