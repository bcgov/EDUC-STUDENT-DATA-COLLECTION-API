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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SpecialEdHeadcountResult;
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
public class SpecialEdHeadcountPerSchoolReportService extends BaseReportGenerationService<SpecialEdHeadcountResult> {

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

    public DownloadableReportResponse generateSpecialEdHeadcountPerSchoolReport(UUID collectionID) {
        try {
            Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
            SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                    new EntityNotFoundException(SdcDistrictCollectionEntity.class, "CollectionId", collectionID.toString()));

            spedHeadcounts = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
            this.allSchoolsTombstones = getAllSchoolTombstones(collectionID);
            return generateJasperReport(convertToReportJSONStringDistrict(spedHeadcounts, sdcDistrictCollectionEntity), specialEdHeadcountPerSchoolReport, ReportTypeCode.DIS_SPECIAL_EDUCATION_HEADCOUNT_PER_SCHOOL);
        } catch (JsonProcessingException e) {
            log.error("Exception occurred while writing PDF report for special ed dis per school :: " + e.getMessage());
            throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for special ed dis per school :: " + e.getMessage());
        }
    }

    public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH) {
        HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
        Set<String> includedSchoolIDs = new HashSet<>();

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

    private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
        nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
        nodeMap.put(sectionPrefix + "level1", new HeadcountChildNode("Level 1", FALSE, sequencePrefix + "1", false));
        nodeMap.put(sectionPrefix + "level2", new HeadcountChildNode("Level 2", FALSE, sequencePrefix + "2", false));
        nodeMap.put(sectionPrefix + "level3", new HeadcountChildNode("Level 3", FALSE, sequencePrefix + "3", false));
        nodeMap.put(sectionPrefix + "other", new HeadcountChildNode("Other", FALSE, sequencePrefix + "4", false));
        nodeMap.put(sectionPrefix + "all", new HeadcountChildNode("All Special Ed Programs", FALSE, sequencePrefix + "5", false));
    }

    public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, SpecialEdHeadcountResult gradeResult) {
        Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
        var code = optionalCode.orElseThrow(() ->
                new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));
        String schoolID = gradeResult.getSchoolID();

        if (nodeMap.containsKey(schoolID + "level1")) {
            nodeMap.get(schoolID + "level1").setValueForGrade(code, gradeResult.getLevelOnes());
        }

        if (nodeMap.containsKey(schoolID + "level2")) {
            nodeMap.get(schoolID + "level2").setValueForGrade(code, gradeResult.getLevelTwos());
        }

        if (nodeMap.containsKey(schoolID + "level3")) {
            nodeMap.get(schoolID + "level3").setValueForGrade(code, gradeResult.getLevelThrees());
        }

        if (nodeMap.containsKey(schoolID + "other")) {
            nodeMap.get(schoolID + "other").setValueForGrade(code, gradeResult.getOtherLevels());
        }

        if (nodeMap.containsKey(schoolID + "all")) {
            nodeMap.get(schoolID + "all").setValueForGrade(code, gradeResult.getAllLevels());
        }

        if (nodeMap.containsKey(schoolID + "Heading")) {
            nodeMap.get(schoolID + "Heading").setAllValuesToNull();
        }
    }
}
