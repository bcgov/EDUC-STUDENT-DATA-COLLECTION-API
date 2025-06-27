package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.helpers.PRPorYouthHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.PRPorYouthHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.*;
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
public class PRPorYouthHeadcountReportService extends BaseReportGenerationService<PRPorYouthHeadcountResult>{

    protected static final String ALLPRPORYOUTH = "allPRPorYouth";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final PRPorYouthHeadcountHelper prpOrYouthHeadcountHelper;
    private final RestUtils restUtils;
    private List<SchoolTombstone> allSchoolsTombstones;
    private List<PRPorYouthHeadcountResult> prpOrYouthHeadcountList;
    private JasperReport prpOrYouthHeadcountReport;

    public PRPorYouthHeadcountReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, PRPorYouthHeadcountHelper prpOrYouthHeadcountHelper, RestUtils restUtils) {
        super(restUtils, sdcSchoolCollectionRepository);
        this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
        this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
        this.prpOrYouthHeadcountHelper = prpOrYouthHeadcountHelper;
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
            InputStream inputYouthPRPHeadcount = getClass().getResourceAsStream("/reports/youthPrpHeadcountsPerSchool.jrxml");
            prpOrYouthHeadcountReport = JasperCompileManager.compileReport(inputYouthPRPHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generatePerSchoolReport(UUID sdcDistrictCollectionID){
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

            Map<String, List<SchoolTombstone>> schoolTombstoneMap = prpOrYouthHeadcountHelper.getAllPRPAndYouthSchoolTombstones(sdcDistrictCollectionID);
            this.allSchoolsTombstones = schoolTombstoneMap.get("ALLPRPORYOUTH");
            List<UUID> youthPRPSchoolIDs = schoolTombstoneMap.get("ALLPRPORYOUTH").stream().map(SchoolTombstone::getSchoolId).map(UUID::fromString).toList();
            List<UUID> youthSchoolIDs = schoolTombstoneMap.get("YOUTH").stream().map(SchoolTombstone::getSchoolId).map(UUID::fromString).toList();
            List<UUID> shortPRPSchoolIDs = schoolTombstoneMap.get("SHORT_PRP").stream().map(SchoolTombstone::getSchoolId).map(UUID::fromString).toList();
            List<UUID> longPRPSchoolIDs = schoolTombstoneMap.get("LONG_PRP").stream().map(SchoolTombstone::getSchoolId).map(UUID::fromString).toList();

            var studentList = sdcSchoolCollectionStudentRepository.getYouthPRPHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(),
                    youthPRPSchoolIDs, youthSchoolIDs, shortPRPSchoolIDs, longPRPSchoolIDs);
            this.prpOrYouthHeadcountList = studentList;
            return generateJasperReport(convertToYouthPRPJSONStringDistrict(studentList, sdcDistrictCollectionEntity), prpOrYouthHeadcountReport, DistrictReportTypeCode.DIS_PRP_OR_YOUTH_SUMMARY.getCode());
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for dis prp youth :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for dis prp youth :: " + e.getMessage());
        }
    }

    private String convertToYouthPRPJSONStringDistrict(List<PRPorYouthHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
        HeadcountNode mainNode = new HeadcountNode();
        HeadcountReportNode reportNode = new HeadcountReportNode();
        setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

        var nodeMap = generateNodeMap(Boolean.TRUE);

        mappedResults.forEach(PRPorYouthHeadcountResult -> setRowValues(nodeMap, PRPorYouthHeadcountResult));

        reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
        mainNode.setReport(reportNode);
        return objectWriter.writeValueAsString(mainNode);
    }

    protected HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLPRPORYOUTH, "All Youth Custody or PRP Students", "00", includeKH);
        int sequencePrefix = 10;

        if (prpOrYouthHeadcountList != null) {
            for (PRPorYouthHeadcountResult result : prpOrYouthHeadcountList) {
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
        if (Objects.equals(sectionPrefix, ALLPRPORYOUTH)) {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, true, includeKH));
        } else {
            nodeMap.put(sectionPrefix, new GradeHeadcountChildNode(sectionTitle, "false", sequencePrefix + "0", false, true, true, includeKH));
        }
    }

    protected void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, PRPorYouthHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

        GradeHeadcountChildNode allYouthPRPNode = (GradeHeadcountChildNode)nodeMap.get(ALLPRPORYOUTH);
        if (allYouthPRPNode.getValueForGrade(code) == null) {
            allYouthPRPNode.setValueForGrade(code, "0");
        }

        String schoolID = gradeResult.getSchoolID();
        if (nodeMap.containsKey(schoolID)) {
            ((GradeHeadcountChildNode)nodeMap.get(schoolID)).setValueForGrade(code, gradeResult.getYouthPRPTotals());
        } else {
            log.warn("School ID {} not found in node map", schoolID);
        }

        int currentTotal = Integer.parseInt(gradeResult.getYouthPRPTotals());
        int accumulatedTotal = Integer.parseInt(allYouthPRPNode.getValueForGrade(code));
        allYouthPRPNode.setValueForGrade(code, String.valueOf(accumulatedTotal + currentTotal));
    }
}
