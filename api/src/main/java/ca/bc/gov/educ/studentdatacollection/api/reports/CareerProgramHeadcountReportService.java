package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class CareerProgramHeadcountReportService extends BaseReportGenerationService<CareerHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport careerProgramHeadcountReport;

  public CareerProgramHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
      super(restUtils, sdcSchoolCollectionRepository);
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
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

  public DownloadableReportResponse generateSchoolCareerProgramHeadcountReport(UUID sdcSchoolCollectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));

      var careerProgramList = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(careerProgramList, sdcSchoolCollectionEntity), careerProgramHeadcountReport, SchoolReportTypeCode.CAREER_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for career programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for career programs :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateDistrictCareerProgramHeadcountReport(UUID sdcDistrictCollectionID){
    try{
      Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional =  sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
      SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));

      List<CareerHeadcountResult> careerHeadcounts = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
      return generateJasperReport(convertToReportJSONStringDistrict(careerHeadcounts, sdcDistrictCollectionEntity), careerProgramHeadcountReport, DistrictReportTypeCode.DIS_CAREER_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for grade enrolment dis :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrolment dis :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "careerPrep", "Career Preparation", "00");
    addValuesForSectionToMap(nodeMap, "coop", "Co-operative Education", "10");
    addValuesForSectionToMap(nodeMap, "techYouth", "Career Technical or Youth Train in Trades", "20");
    addValuesForSectionToMap(nodeMap, "youthWorkInTrades", "Youth Work in Trades Program", "30");
    addValuesForSectionToMap(nodeMap, "all", "All Career Programs", "40");

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
    nodeMap.put(sectionPrefix + "Heading", new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, false, false));
    nodeMap.put(sectionPrefix + "XA", new GradeHeadcountChildNode("XA - Business & Applied Business", FALSE, sequencePrefix + "1", false, true, false, false));
    nodeMap.put(sectionPrefix + "XB", new GradeHeadcountChildNode("XB - Fine Arts, Design & Media", FALSE, sequencePrefix + "2", false, true, false, false));
    nodeMap.put(sectionPrefix + "XC", new GradeHeadcountChildNode("XC - Fitness & Recreation", FALSE, sequencePrefix + "3", false, true, false, false));
    nodeMap.put(sectionPrefix + "XD", new GradeHeadcountChildNode("XD - Health & Human Services", FALSE, sequencePrefix + "4", false, true, false, false));
    nodeMap.put(sectionPrefix + "XE", new GradeHeadcountChildNode("XE - Liberal Arts & Humanities", FALSE, sequencePrefix + "5", false, true, false, false));
    nodeMap.put(sectionPrefix + "XF", new GradeHeadcountChildNode("XF - Science & Applied Science", FALSE, sequencePrefix + "6", false, true, false, false));
    nodeMap.put(sectionPrefix + "XG", new GradeHeadcountChildNode("XG - Tourism, Hospitality & Foods", FALSE, sequencePrefix + "7", false, true, false, false));
    nodeMap.put(sectionPrefix + "XH", new GradeHeadcountChildNode("XH - Trades & Technology", FALSE, sequencePrefix + "8", false, true, false, false));
  }

  public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, CareerHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("careerPrepHeading")).setValueForGrade(code, gradeResult.getPreparationTotal());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXA")).setValueForGrade(code, gradeResult.getPreparationXA());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXB")).setValueForGrade(code, gradeResult.getPreparationXB());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXC")).setValueForGrade(code, gradeResult.getPreparationXC());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXD")).setValueForGrade(code, gradeResult.getPreparationXD());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXE")).setValueForGrade(code, gradeResult.getPreparationXE());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXF")).setValueForGrade(code, gradeResult.getPreparationXF());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXG")).setValueForGrade(code, gradeResult.getPreparationXG());
    ((GradeHeadcountChildNode)nodeMap.get("careerPrepXH")).setValueForGrade(code, gradeResult.getPreparationXH());

    ((GradeHeadcountChildNode)nodeMap.get("coopHeading")).setValueForGrade(code, gradeResult.getCoopTotal());
    ((GradeHeadcountChildNode)nodeMap.get("coopXA")).setValueForGrade(code, gradeResult.getCoopXA());
    ((GradeHeadcountChildNode)nodeMap.get("coopXB")).setValueForGrade(code, gradeResult.getCoopXB());
    ((GradeHeadcountChildNode)nodeMap.get("coopXC")).setValueForGrade(code, gradeResult.getCoopXC());
    ((GradeHeadcountChildNode)nodeMap.get("coopXD")).setValueForGrade(code, gradeResult.getCoopXD());
    ((GradeHeadcountChildNode)nodeMap.get("coopXE")).setValueForGrade(code, gradeResult.getCoopXE());
    ((GradeHeadcountChildNode)nodeMap.get("coopXF")).setValueForGrade(code, gradeResult.getCoopXF());
    ((GradeHeadcountChildNode)nodeMap.get("coopXG")).setValueForGrade(code, gradeResult.getCoopXG());
    ((GradeHeadcountChildNode)nodeMap.get("coopXH")).setValueForGrade(code, gradeResult.getCoopXH());

    ((GradeHeadcountChildNode)nodeMap.get("techYouthHeading")).setValueForGrade(code, gradeResult.getTechYouthTotal());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXA")).setValueForGrade(code, gradeResult.getTechYouthXA());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXB")).setValueForGrade(code, gradeResult.getTechYouthXB());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXC")).setValueForGrade(code, gradeResult.getTechYouthXC());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXD")).setValueForGrade(code, gradeResult.getTechYouthXD());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXE")).setValueForGrade(code, gradeResult.getTechYouthXE());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXF")).setValueForGrade(code, gradeResult.getTechYouthXF());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXG")).setValueForGrade(code, gradeResult.getTechYouthXG());
    ((GradeHeadcountChildNode)nodeMap.get("techYouthXH")).setValueForGrade(code, gradeResult.getTechYouthXH());

    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesHeading")).setValueForGrade(code, gradeResult.getApprenticeTotal());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXA")).setValueForGrade(code, gradeResult.getApprenticeXA());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXB")).setValueForGrade(code, gradeResult.getApprenticeXB());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXC")).setValueForGrade(code, gradeResult.getApprenticeXC());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXD")).setValueForGrade(code, gradeResult.getApprenticeXD());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXE")).setValueForGrade(code, gradeResult.getApprenticeXE());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXF")).setValueForGrade(code, gradeResult.getApprenticeXF());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXG")).setValueForGrade(code, gradeResult.getApprenticeXG());
    ((GradeHeadcountChildNode)nodeMap.get("youthWorkInTradesXH")).setValueForGrade(code, gradeResult.getApprenticeXH());

    ((GradeHeadcountChildNode)nodeMap.get("allHeading")).setValueForGrade(code, gradeResult.getAllTotal());
    ((GradeHeadcountChildNode)nodeMap.get("allXA")).setValueForGrade(code, gradeResult.getAllXA());
    ((GradeHeadcountChildNode)nodeMap.get("allXB")).setValueForGrade(code, gradeResult.getAllXB());
    ((GradeHeadcountChildNode)nodeMap.get("allXC")).setValueForGrade(code, gradeResult.getAllXC());
    ((GradeHeadcountChildNode)nodeMap.get("allXD")).setValueForGrade(code, gradeResult.getAllXD());
    ((GradeHeadcountChildNode)nodeMap.get("allXE")).setValueForGrade(code, gradeResult.getAllXE());
    ((GradeHeadcountChildNode)nodeMap.get("allXF")).setValueForGrade(code, gradeResult.getAllXF());
    ((GradeHeadcountChildNode)nodeMap.get("allXG")).setValueForGrade(code, gradeResult.getAllXG());
    ((GradeHeadcountChildNode)nodeMap.get("allXH")).setValueForGrade(code, gradeResult.getAllXH());
  }

}
