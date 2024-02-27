package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.CareerHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.careerprogramheadcount.CareerProgramHeadcountCareerProgramNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.careerprogramheadcount.CareerProgramHeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.careerprogramheadcount.CareerProgramHeadcountReportNode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CareerProgramHeadcountReportService extends BaseReportGenerationService{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport careerProgramHeadcountReport;
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

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

  public String generateCareerProgramHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var careerProgramList = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToCareerProgramReportJSONString(careerProgramList, sdcSchoolCollectionEntity), careerProgramHeadcountReport);
    } catch (JsonProcessingException e) {
      log.info("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  private String convertToCareerProgramReportJSONString(List<CareerHeadcountResult> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    CareerProgramHeadcountNode mainNode = new CareerProgramHeadcountNode();
    CareerProgramHeadcountReportNode reportNode = new CareerProgramHeadcountReportNode();
    setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap();

    mappedResults.forEach(careerHeadcountResult -> setValueForGrade(nodeMap, careerHeadcountResult));

    reportNode.setCareerPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).collect(Collectors.toList()));
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private HashMap<String, CareerProgramHeadcountCareerProgramNode> generateNodeMap(){
    HashMap<String, CareerProgramHeadcountCareerProgramNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "careerPrep", "Career Preparation", "00");
    addValuesForSectionToMap(nodeMap, "coop", "Co-operative Education", "10");
    addValuesForSectionToMap(nodeMap, "techYouth", "Career Technical or youth Train in Trades", "20");
    addValuesForSectionToMap(nodeMap, "apprentice", "Apprenticeship", "30");
    addValuesForSectionToMap(nodeMap, "all", "All Career Programs", "40");

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, CareerProgramHeadcountCareerProgramNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
    nodeMap.put(sectionPrefix + "Heading", new CareerProgramHeadcountCareerProgramNode(sectionTitle, "true", sequencePrefix + "0"));
    nodeMap.put(sectionPrefix + "XA", new CareerProgramHeadcountCareerProgramNode("XA - Business & Applied Business", FALSE, sequencePrefix + "1"));
    nodeMap.put(sectionPrefix + "XB", new CareerProgramHeadcountCareerProgramNode("XB - Fine Arts, Design & Media", FALSE, sequencePrefix + "2"));
    nodeMap.put(sectionPrefix + "XC", new CareerProgramHeadcountCareerProgramNode("XC - Fitness & Recreation", FALSE, sequencePrefix + "3"));
    nodeMap.put(sectionPrefix + "XD", new CareerProgramHeadcountCareerProgramNode("XD - Health & Human Services", FALSE, sequencePrefix + "4"));
    nodeMap.put(sectionPrefix + "XE", new CareerProgramHeadcountCareerProgramNode("XE - Liberal Arts & Humanities", FALSE, sequencePrefix + "5"));
    nodeMap.put(sectionPrefix + "XF", new CareerProgramHeadcountCareerProgramNode("XF - Science & Applied Science", FALSE, sequencePrefix + "6"));
    nodeMap.put(sectionPrefix + "XG", new CareerProgramHeadcountCareerProgramNode("XG - Tourism, Hospitality & Foods", FALSE, sequencePrefix + "7"));
    nodeMap.put(sectionPrefix + "XH", new CareerProgramHeadcountCareerProgramNode("XH - Trades & Technology", FALSE, sequencePrefix + "8"));
  }

  private void setValueForGrade(HashMap<String, CareerProgramHeadcountCareerProgramNode> nodeMap, CareerHeadcountResult gradeResult){
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
