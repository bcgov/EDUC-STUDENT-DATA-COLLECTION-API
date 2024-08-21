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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.CareerHeadcountResult;
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
public class CareerProgramHeadcountPerSchoolReportService extends BaseReportGenerationService<CareerHeadcountResult>{

  public static final String YOUTH_WORK_IN_TRADES = "youthWorkInTrades";
  public static final String CAREER_PREP = "careerPrep";
  public static final String HEADING = "Heading";
  public static final String COOP = "coop";
  public static final String TECH_YOUTH = "techYouth";
  public static final String ALL = "all";
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport careerProgramHeadcountPerSchoolReport;
  private final RestUtils restUtils;
  private List<CareerHeadcountResult> careerHeadcounts = new ArrayList<>();
  private List<SchoolTombstone> allSchoolsTombstones;

  public CareerProgramHeadcountPerSchoolReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils1) {
      super(restUtils, sdcSchoolCollectionRepository);
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
      this.restUtils = restUtils1;
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
      InputStream inputCareerProgramHeadcount = getClass().getResourceAsStream("/reports/careerProgramHeadcountsPerSchool.jrxml");
      careerProgramHeadcountPerSchoolReport = JasperCompileManager.compileReport(inputCareerProgramHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateCareerProgramHeadcountPerSchoolReport(UUID collectionID){
      try {
        Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional =  sdcDistrictCollectionRepository.findById(collectionID);
        SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
                new EntityNotFoundException(SdcDistrictCollectionEntity.class, "CollectionId", collectionID.toString()));

        careerHeadcounts = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
        this.allSchoolsTombstones = getAllSchoolTombstones(collectionID);
        return generateJasperReport(convertToReportJSONStringDistrict(careerHeadcounts, sdcDistrictCollectionEntity), careerProgramHeadcountPerSchoolReport, ReportTypeCode.DIS_CAREER_HEADCOUNT_PER_SCHOOL);
      } catch (JsonProcessingException e) {
        log.error("Exception occurred while writing PDF report for grade enrollment dis :: " + e.getMessage());
        throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment dis :: " + e.getMessage());
      }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    Set<String> includedSchoolIDs = new HashSet<>();

    int sequencePrefix = 10;
    if (!careerHeadcounts.isEmpty()) {
      for (CareerHeadcountResult result : careerHeadcounts) {
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
    nodeMap.put(sectionPrefix + HEADING, new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
    nodeMap.put(sectionPrefix + CAREER_PREP, new GradeHeadcountChildNode("Career Preparation", FALSE, sequencePrefix + "1", false));
    nodeMap.put(sectionPrefix + COOP, new GradeHeadcountChildNode("Co-operative Education", FALSE, sequencePrefix + "2", false));
    nodeMap.put(sectionPrefix + TECH_YOUTH, new GradeHeadcountChildNode("Career Technical or Youth Train in Trades", FALSE, sequencePrefix + "3", false));
    nodeMap.put(sectionPrefix + YOUTH_WORK_IN_TRADES, new GradeHeadcountChildNode("Youth Work in Trades Program", FALSE, sequencePrefix + "4", false));
    nodeMap.put(sectionPrefix + ALL, new GradeHeadcountChildNode("All Career Programs", FALSE, sequencePrefix + "5", false));
  }

  public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, CareerHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    String schoolID = gradeResult.getSchoolID();
    if (nodeMap.containsKey(schoolID + CAREER_PREP)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + CAREER_PREP)).setValueForGrade(code, gradeResult.getPreparationTotal());
    }

    if (nodeMap.containsKey(schoolID + COOP)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + COOP)).setValueForGrade(code, gradeResult.getCoopTotal());
    }

    if (nodeMap.containsKey(schoolID + TECH_YOUTH)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + TECH_YOUTH)).setValueForGrade(code, gradeResult.getTechYouthTotal());
    }

    if (nodeMap.containsKey(schoolID + YOUTH_WORK_IN_TRADES)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + YOUTH_WORK_IN_TRADES)).setValueForGrade(code, gradeResult.getApprenticeTotal());
    }

    if (nodeMap.containsKey(schoolID + ALL)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + ALL)).setValueForGrade(code, gradeResult.getAllTotal());
    }

    if (nodeMap.containsKey(schoolID + HEADING)) {
      ((GradeHeadcountChildNode)nodeMap.get(schoolID + HEADING)).setAllValuesToNull();
    }

  }

}
