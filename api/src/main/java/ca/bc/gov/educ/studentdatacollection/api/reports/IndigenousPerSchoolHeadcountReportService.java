package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndigenousHeadcountResult;
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
public class IndigenousPerSchoolHeadcountReportService extends BaseReportGenerationService<IndigenousHeadcountResult> {

    protected static final String ALLIND = "allInd";
    public static final String IND_SUPPORT = "indSupport";
    public static final String IND_LANG = "indLang";
    public static final String HEADING = "Heading";
    public static final String IND_PROG = "indProg";
    public static final String ALL = "all";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport indHeadcountPerSchoolReport;
    private final RestUtils restUtils;
    private List<IndigenousHeadcountResult> indHeadcounts = new ArrayList<>();
    private List<SchoolTombstone> allSchoolsTombstones;

    public IndigenousPerSchoolHeadcountReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, RestUtils restUtils1) {
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
            InputStream inputSpecialEdHeadcount = getClass().getResourceAsStream("/reports/indigenousHeadcountsPerSchool.jrxml");
            indHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputSpecialEdHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateIndigenousHeadcountPerSchoolReport(UUID sdcDistrictCollectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            indHeadcounts = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(sdcDistrictCollectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(indHeadcounts, sdcDistrictCollectionEntity), indHeadcountPerSchoolReport, DistrictReportTypeCode.DIS_INDIGENOUS_HEADCOUNT_PER_SCHOOL.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education dis per school :: " + e.getMessage());
        }
    }

    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLIND, "All Indigenous Support Program Headcount for All Schools", "00");

        int sequencePrefix = 10;
        if (!indHeadcounts.isEmpty()) {
            for (IndigenousHeadcountResult result : indHeadcounts) {
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
        if (Objects.equals(sectionPrefix, ALLIND)) {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, false, false, false));
        } else {
            nodeMap.put(sectionPrefix + HEADING, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
            nodeMap.put(sectionPrefix + IND_LANG, new GradeHeadcountChildNode("Indigenous Language and Culture", FALSE, sequencePrefix + "1", false));
            nodeMap.put(sectionPrefix + IND_SUPPORT, new GradeHeadcountChildNode("Indigenous Support Services", FALSE, sequencePrefix + "2", false));
            nodeMap.put(sectionPrefix + IND_PROG, new GradeHeadcountChildNode("Other Approved Indigenous Programs", FALSE, sequencePrefix + "3", false));
            nodeMap.put(sectionPrefix + ALL, new GradeHeadcountChildNode("All Indigenous Support Programs", FALSE, sequencePrefix + "4", false));
        }
    }

    public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, IndigenousHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));
        String schoolID = gradeResult.getSchoolID();

        GradeHeadcountChildNode allIndNode = (GradeHeadcountChildNode)nodeMap.get(ALLIND);
        if (allIndNode.getValueForGrade(code) == null) {
            allIndNode.setValueForGrade(code, "0");
        }

        if (nodeMap.containsKey(schoolID + IND_LANG)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + IND_LANG)).setValueForGrade(code, gradeResult.getIndigenousLanguageTotal());
        }

        if (nodeMap.containsKey(schoolID + IND_SUPPORT)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + IND_SUPPORT)).setValueForGrade(code, gradeResult.getIndigenousSupportTotal());
        }

        if (nodeMap.containsKey(schoolID + IND_PROG)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + IND_PROG)).setValueForGrade(code, gradeResult.getOtherProgramTotal());
        }

        if (nodeMap.containsKey(schoolID + ALL)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + ALL)).setValueForGrade(code, gradeResult.getAllSupportProgramTotal());
        }

        if (nodeMap.containsKey(schoolID + HEADING)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID + HEADING)).setAllValuesToNull();
        }

        int currentTotal = Integer.parseInt(gradeResult.getAllSupportProgramTotal());
        int accumulatedTotal = Integer.parseInt(allIndNode.getValueForGrade(code));
        allIndNode.setValueForGrade(code, String.valueOf(accumulatedTotal + currentTotal));
    }
}
