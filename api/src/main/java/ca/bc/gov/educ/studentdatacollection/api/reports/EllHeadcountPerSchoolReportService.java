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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EllHeadcountResult;
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
public class EllHeadcountPerSchoolReportService extends BaseReportGenerationService<EllHeadcountResult> {
    protected static final String ALLELL = "allEll";
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private JasperReport ellHeadcountPerSchoolReport;
    private final RestUtils restUtils;
    private List<EllHeadcountResult> ellHeadcounts = new ArrayList<>();
    private List<SchoolTombstone> allSchoolsTombstones;

    public EllHeadcountPerSchoolReportService(SdcDistrictCollectionRepository sdcDistrictCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository, RestUtils restUtils1) {
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
            ellHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputSpecialEdHeadcount);
        } catch (JRException e) {
            throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
        }
    }

    public DownloadableReportResponse generateEllHeadcountPerSchoolReport(UUID collectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "CollectionId", collectionID.toString()));

            ellHeadcounts = sdcSchoolCollectionStudentRepository.getEllHeadcountsByBySchoolIdAndSdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(collectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(ellHeadcounts, sdcDistrictCollectionEntity), ellHeadcountPerSchoolReport, ReportTypeCode.DIS_ELL_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for ell dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for ell dis per school :: " + e.getMessage());
        }
    }

    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();
        addValuesForSectionToMap(nodeMap, ALLELL, "All English Language Learners Headcount for All Schools", "00");

        int sequencePrefix = 10;
        if (!ellHeadcounts.isEmpty()) {
            for (EllHeadcountResult result : ellHeadcounts) {
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
        if (Objects.equals(sectionPrefix, ALLELL)) {
            nodeMap.put(sectionPrefix, new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, false, false, false));
        } else {
            nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
            nodeMap.put(sectionPrefix + "all", new HeadcountChildNode("All English Language Learners", FALSE, sequencePrefix + "1", false));
        }
    }

    public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, EllHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));
        String schoolID = gradeResult.getSchoolID();

        HeadcountChildNode allEllNode = nodeMap.get(ALLELL);
        if (allEllNode.getValueForGrade(code) == null) {
            allEllNode.setValueForGrade(code, "0");
        }

        if (nodeMap.containsKey(schoolID + "all")) {
            nodeMap.get(schoolID + "all").setValueForGrade(code, gradeResult.getTotalEllStudents());
        }

        if (nodeMap.containsKey(schoolID + "Heading")) {
            nodeMap.get(schoolID + "Heading").setAllValuesToNull();
        }

        int currentTotal = Integer.parseInt(gradeResult.getTotalEllStudents());
        int accumulatedTotal = Integer.parseInt(allEllNode.getValueForGrade(code));
        allEllNode.setValueForGrade(code, String.valueOf(accumulatedTotal + currentTotal));
    }
}