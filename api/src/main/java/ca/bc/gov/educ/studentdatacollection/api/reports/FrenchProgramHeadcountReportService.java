package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.frenchprogramheadcount.FrenchProgramHeadcountFrenchProgramNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.frenchprogramheadcount.FrenchProgramHeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.frenchprogramheadcount.FrenchProgramHeadcountReportNode;
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
public class FrenchProgramHeadcountReportService extends BaseReportGenerationService{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport frenchProgramHeadcountReport;
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  public FrenchProgramHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
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
      InputStream inputFrenchProgramHeadcount = getClass().getResourceAsStream("/reports/frenchProgramHeadcounts.jrxml");
      frenchProgramHeadcountReport = JasperCompileManager.compileReport(inputFrenchProgramHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public String generateFrenchProgramHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var frenchProgramList = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToFrenchProgramReportJSONString(frenchProgramList, sdcSchoolCollectionEntity), frenchProgramHeadcountReport);
    } catch (JsonProcessingException e) {
      log.info("Exception occurred while writing PDF report for french programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for french programs :: " + e.getMessage());
    }
  }

  private String convertToFrenchProgramReportJSONString(List<FrenchHeadcountResult> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    FrenchProgramHeadcountNode mainNode = new FrenchProgramHeadcountNode();
    FrenchProgramHeadcountReportNode reportNode = new FrenchProgramHeadcountReportNode();
    setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap();

    mappedResults.forEach(frenchHeadcountResult -> setValueForGrade(nodeMap, frenchHeadcountResult));

    reportNode.setFrenchPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private HashMap<String, FrenchProgramHeadcountFrenchProgramNode> generateNodeMap(){
    HashMap<String, FrenchProgramHeadcountFrenchProgramNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "coreFrench", "Core French", "00");
    addValuesForSectionToMap(nodeMap, "earlyFrenchImmersion", "Early French Immersion", "10");
    addValuesForSectionToMap(nodeMap, "lateFrenchImmersion", "Late French Immersion", "20");
    addValuesForSectionToMap(nodeMap, "allFrenchPrograms", "All French Programs", "30");

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, FrenchProgramHeadcountFrenchProgramNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
    nodeMap.put(sectionPrefix + "Heading", new FrenchProgramHeadcountFrenchProgramNode(sectionTitle, "true", sequencePrefix + "0"));
    nodeMap.put(sectionPrefix + "SchoolAged", new FrenchProgramHeadcountFrenchProgramNode("School-Aged", FALSE, sequencePrefix + "1"));
    nodeMap.put(sectionPrefix + "Adult", new FrenchProgramHeadcountFrenchProgramNode("Adult", FALSE, sequencePrefix + "2"));
  }

  private void setValueForGrade(HashMap<String, FrenchProgramHeadcountFrenchProgramNode> nodeMap, FrenchHeadcountResult frenchHeadcountResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(frenchHeadcountResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", frenchHeadcountResult.getEnrolledGradeCode()));

    nodeMap.get("coreFrenchHeading").setValueForGrade(code, frenchHeadcountResult.getTotalCoreFrench());
    nodeMap.get("coreFrenchSchoolAged").setValueForGrade(code, frenchHeadcountResult.getSchoolAgedCoreFrench());
    nodeMap.get("coreFrenchAdult").setValueForGrade(code, frenchHeadcountResult.getAdultCoreFrench());

    nodeMap.get("earlyFrenchImmersionHeading").setValueForGrade(code, frenchHeadcountResult.getTotalEarlyFrench());
    nodeMap.get("earlyFrenchImmersionSchoolAged").setValueForGrade(code, frenchHeadcountResult.getSchoolAgedEarlyFrench());
    nodeMap.get("earlyFrenchImmersionAdult").setValueForGrade(code, frenchHeadcountResult.getAdultEarlyFrench());

    nodeMap.get("lateFrenchImmersionHeading").setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());
    nodeMap.get("lateFrenchImmersionSchoolAged").setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());
    nodeMap.get("lateFrenchImmersionAdult").setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());

    nodeMap.get("allFrenchProgramsHeading").setValueForGrade(code, frenchHeadcountResult.getTotalTotals());
    nodeMap.get("allFrenchProgramsSchoolAged").setValueForGrade(code, frenchHeadcountResult.getSchoolAgedTotals());
    nodeMap.get("allFrenchProgramsAdult").setValueForGrade(code, frenchHeadcountResult.getAdultTotals());
  }

}
