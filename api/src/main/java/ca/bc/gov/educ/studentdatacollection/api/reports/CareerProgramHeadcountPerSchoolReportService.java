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
    nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
    nodeMap.put(sectionPrefix + "careerPrep", new HeadcountChildNode("Career Preparation", FALSE, sequencePrefix + "1", false));
    nodeMap.put(sectionPrefix + "coop", new HeadcountChildNode("Co-operative Education", FALSE, sequencePrefix + "2", false));
    nodeMap.put(sectionPrefix + "techYouth", new HeadcountChildNode("Career Technical or Youth Train in Trades", FALSE, sequencePrefix + "3", false));
    nodeMap.put(sectionPrefix + "youthWorkInTrades", new HeadcountChildNode("Youth Work in Trades Program", FALSE, sequencePrefix + "4", false));
    nodeMap.put(sectionPrefix + "all", new HeadcountChildNode("All Career Programs", FALSE, sequencePrefix + "5", false));
  }

  public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, CareerHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    String schoolID = gradeResult.getSchoolID();
    if (nodeMap.containsKey(schoolID + "careerPrep")) {
      nodeMap.get(schoolID + "careerPrep").setValueForGrade(code, gradeResult.getPreparationTotal());
    }

    if (nodeMap.containsKey(schoolID + "coop")) {
      nodeMap.get(schoolID + "coop").setValueForGrade(code, gradeResult.getCoopTotal());
    }

    if (nodeMap.containsKey(schoolID + "techYouth")) {
      nodeMap.get(schoolID + "techYouth").setValueForGrade(code, gradeResult.getTechYouthTotal());
    }

    if (nodeMap.containsKey(schoolID + "youthWorkInTrades")) {
      nodeMap.get(schoolID + "youthWorkInTrades").setValueForGrade(code, gradeResult.getApprenticeTotal());
    }

    if (nodeMap.containsKey(schoolID + "all")) {
      nodeMap.get(schoolID + "all").setValueForGrade(code, gradeResult.getAllTotal());
    }

    if (nodeMap.containsKey(schoolID + "Heading")) {
      nodeMap.get(schoolID + "Heading").setAllValuesToNull();
    }

  }

}
