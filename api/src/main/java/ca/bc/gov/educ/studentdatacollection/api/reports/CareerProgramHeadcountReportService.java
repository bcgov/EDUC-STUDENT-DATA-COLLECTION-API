package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
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
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CareerProgramHeadcountReportService extends BaseReportGenerationService<CareerHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport careerProgramHeadcountReport;

  public CareerProgramHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
      super(restUtils);
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
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
      InputStream inputCareerProgramHeadcount = getClass().getResourceAsStream("/reports/careerProgramHeadcounts.jrxml");
      careerProgramHeadcountReport = JasperCompileManager.compileReport(inputCareerProgramHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateCareerProgramHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var careerProgramList = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(careerProgramList, sdcSchoolCollectionEntity), careerProgramHeadcountReport, ReportTypeCode.CAREER_HEADCOUNT);
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for career programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for career programs :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "careerPrep", "Career Preparation", "00");
    addValuesForSectionToMap(nodeMap, "coop", "Co-operative Education", "10");
    addValuesForSectionToMap(nodeMap, "techYouth", "Career Technical or youth Train in Trades", "20");
    addValuesForSectionToMap(nodeMap, "apprentice", "Apprenticeship", "30");
    addValuesForSectionToMap(nodeMap, "all", "All Career Programs", "40");

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
    nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
    nodeMap.put(sectionPrefix + "XA", new HeadcountChildNode("XA - Business & Applied Business", FALSE, sequencePrefix + "1", false));
    nodeMap.put(sectionPrefix + "XB", new HeadcountChildNode("XB - Fine Arts, Design & Media", FALSE, sequencePrefix + "2", false));
    nodeMap.put(sectionPrefix + "XC", new HeadcountChildNode("XC - Fitness & Recreation", FALSE, sequencePrefix + "3", false));
    nodeMap.put(sectionPrefix + "XD", new HeadcountChildNode("XD - Health & Human Services", FALSE, sequencePrefix + "4", false));
    nodeMap.put(sectionPrefix + "XE", new HeadcountChildNode("XE - Liberal Arts & Humanities", FALSE, sequencePrefix + "5", false));
    nodeMap.put(sectionPrefix + "XF", new HeadcountChildNode("XF - Science & Applied Science", FALSE, sequencePrefix + "6", false));
    nodeMap.put(sectionPrefix + "XG", new HeadcountChildNode("XG - Tourism, Hospitality & Foods", FALSE, sequencePrefix + "7", false));
    nodeMap.put(sectionPrefix + "XH", new HeadcountChildNode("XH - Trades & Technology", FALSE, sequencePrefix + "8", false));
  }

  public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, CareerHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    nodeMap.get("careerPrepHeading").setValueForGrade(code, gradeResult.getPreparationTotal());
    nodeMap.get("careerPrepXA").setValueForGrade(code, gradeResult.getPreparationXA());
    nodeMap.get("careerPrepXB").setValueForGrade(code, gradeResult.getPreparationXB());
    nodeMap.get("careerPrepXC").setValueForGrade(code, gradeResult.getPreparationXC());
    nodeMap.get("careerPrepXD").setValueForGrade(code, gradeResult.getPreparationXD());
    nodeMap.get("careerPrepXE").setValueForGrade(code, gradeResult.getPreparationXE());
    nodeMap.get("careerPrepXF").setValueForGrade(code, gradeResult.getPreparationXF());
    nodeMap.get("careerPrepXG").setValueForGrade(code, gradeResult.getPreparationXG());
    nodeMap.get("careerPrepXH").setValueForGrade(code, gradeResult.getPreparationXH());

    nodeMap.get("coopHeading").setValueForGrade(code, gradeResult.getCoopTotal());
    nodeMap.get("coopXA").setValueForGrade(code, gradeResult.getCoopXA());
    nodeMap.get("coopXB").setValueForGrade(code, gradeResult.getCoopXB());
    nodeMap.get("coopXC").setValueForGrade(code, gradeResult.getCoopXC());
    nodeMap.get("coopXD").setValueForGrade(code, gradeResult.getCoopXD());
    nodeMap.get("coopXE").setValueForGrade(code, gradeResult.getCoopXE());
    nodeMap.get("coopXF").setValueForGrade(code, gradeResult.getCoopXF());
    nodeMap.get("coopXG").setValueForGrade(code, gradeResult.getCoopXG());
    nodeMap.get("coopXH").setValueForGrade(code, gradeResult.getCoopXH());

    nodeMap.get("techYouthHeading").setValueForGrade(code, gradeResult.getTechYouthTotal());
    nodeMap.get("techYouthXA").setValueForGrade(code, gradeResult.getTechYouthXA());
    nodeMap.get("techYouthXB").setValueForGrade(code, gradeResult.getTechYouthXB());
    nodeMap.get("techYouthXC").setValueForGrade(code, gradeResult.getTechYouthXC());
    nodeMap.get("techYouthXD").setValueForGrade(code, gradeResult.getTechYouthXD());
    nodeMap.get("techYouthXE").setValueForGrade(code, gradeResult.getTechYouthXE());
    nodeMap.get("techYouthXF").setValueForGrade(code, gradeResult.getTechYouthXF());
    nodeMap.get("techYouthXG").setValueForGrade(code, gradeResult.getTechYouthXG());
    nodeMap.get("techYouthXH").setValueForGrade(code, gradeResult.getTechYouthXH());

    nodeMap.get("apprenticeHeading").setValueForGrade(code, gradeResult.getApprenticeTotal());
    nodeMap.get("apprenticeXA").setValueForGrade(code, gradeResult.getApprenticeXA());
    nodeMap.get("apprenticeXB").setValueForGrade(code, gradeResult.getApprenticeXB());
    nodeMap.get("apprenticeXC").setValueForGrade(code, gradeResult.getApprenticeXC());
    nodeMap.get("apprenticeXD").setValueForGrade(code, gradeResult.getApprenticeXD());
    nodeMap.get("apprenticeXE").setValueForGrade(code, gradeResult.getApprenticeXE());
    nodeMap.get("apprenticeXF").setValueForGrade(code, gradeResult.getApprenticeXF());
    nodeMap.get("apprenticeXG").setValueForGrade(code, gradeResult.getApprenticeXG());
    nodeMap.get("apprenticeXH").setValueForGrade(code, gradeResult.getApprenticeXH());

    nodeMap.get("allHeading").setValueForGrade(code, gradeResult.getAllTotal());
    nodeMap.get("allXA").setValueForGrade(code, gradeResult.getAllXA());
    nodeMap.get("allXB").setValueForGrade(code, gradeResult.getAllXB());
    nodeMap.get("allXC").setValueForGrade(code, gradeResult.getAllXC());
    nodeMap.get("allXD").setValueForGrade(code, gradeResult.getAllXD());
    nodeMap.get("allXE").setValueForGrade(code, gradeResult.getAllXE());
    nodeMap.get("allXF").setValueForGrade(code, gradeResult.getAllXF());
    nodeMap.get("allXG").setValueForGrade(code, gradeResult.getAllXG());
    nodeMap.get("allXH").setValueForGrade(code, gradeResult.getAllXH());
  }

}
