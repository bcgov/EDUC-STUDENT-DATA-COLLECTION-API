package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.CsfFrenchHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchCombinedHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.FrenchHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.*;
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
import java.util.*;

@Service
@Slf4j
public class FrenchProgramHeadcountReportService extends BaseReportGenerationService<FrenchHeadcountResult>{

  protected static final String GRADEVALUE = "Grade Value";
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private final RestUtils restUtils;
  private JasperReport frenchProgramHeadcountReport;
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  public FrenchProgramHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
      super(restUtils, sdcSchoolCollectionRepository);
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
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
      InputStream inputFrenchProgramHeadcount = getClass().getResourceAsStream("/reports/frenchProgramHeadcounts.jrxml");
      frenchProgramHeadcountReport = JasperCompileManager.compileReport(inputFrenchProgramHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateFrenchProgramHeadcountReport(UUID collectionID, Boolean isDistrict){
    if (Boolean.TRUE.equals(isDistrict)) {
      try {
        Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(collectionID);
        SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcDistrictCollectionEntity.class, "Collection by Id", collectionID.toString()));

        var frenchProgramList = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
        return generateJasperReport(convertToFrenchProgramReportJSONStringDistrict(frenchProgramList, sdcDistrictCollectionEntity), frenchProgramHeadcountReport, ReportTypeCode.DIS_FRENCH_HEADCOUNT);
      } catch (JsonProcessingException e) {
        log.error("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
        throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for dis french programs :: " + e.getMessage());
      }
    } else {
      try {
        Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional = sdcSchoolCollectionRepository.findById(collectionID);
        SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
                new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

        var school = restUtils.getSchoolBySchoolID(sdcSchoolCollectionEntity.getSchoolID().toString());

        if (school.get().getSchoolReportingRequirementCode().equalsIgnoreCase(SchoolReportingRequirementCodes.CSF.getCode())) {
          var frenchProgramList = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
          return generateJasperReport(convertToCSFFrenchProgramReportJSONString(frenchProgramList, sdcSchoolCollectionEntity, school.get()), frenchProgramHeadcountReport, ReportTypeCode.FRENCH_HEADCOUNT);
        } else {
          var frenchProgramList = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
          return generateJasperReport(convertToFrenchProgramReportJSONString(frenchProgramList, sdcSchoolCollectionEntity, school.get()), frenchProgramHeadcountReport, ReportTypeCode.FRENCH_HEADCOUNT);
        }
      } catch (JsonProcessingException e) {
        log.error("Exception occurred while writing PDF report for sch french programs :: " + e.getMessage());
        throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for school french programs :: " + e.getMessage());
      }
    }
  }

  private String convertToCSFFrenchProgramReportJSONString(List<CsfFrenchHeadcountResult> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection, SchoolTombstone schoolTombstone) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMapForCSF(isIndependentSchool(schoolTombstone));

    mappedResults.forEach(frenchHeadcountResult -> setRowValues(nodeMap, frenchHeadcountResult));

    reportNode.setPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private String convertToFrenchProgramReportJSONString(List<FrenchHeadcountResult> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection, SchoolTombstone schoolTombstone) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap(isIndependentSchool(schoolTombstone));

    mappedResults.forEach(frenchHeadcountResult -> setRowValues(nodeMap, frenchHeadcountResult));

    reportNode.setPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private String convertToFrenchProgramReportJSONStringDistrict(List<FrenchCombinedHeadcountResult> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

    var nodeMap = generateNodeMapForDis(false);

    mappedResults.forEach(combinedFrenchHeadcountResult -> setRowValues(nodeMap, combinedFrenchHeadcountResult));

    reportNode.setPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private HashMap<String, HeadcountChildNode> generateNodeMapForCSF(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "csf", "Programme Francophone", "30", includeKH);

    return nodeMap;
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "coreFrench", "Core French", "00", includeKH);
    addValuesForSectionToMap(nodeMap, "earlyFrenchImmersion", "Early French Immersion", "10", includeKH);
    addValuesForSectionToMap(nodeMap, "lateFrenchImmersion", "Late French Immersion", "20", includeKH);
    addValuesForSectionToMap(nodeMap, "allFrenchPrograms", "All French Programs", "30", includeKH);

    return nodeMap;
  }

  public Map<String, HeadcountChildNode> generateNodeMapForDis(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "coreFrench", "Core French", "00", includeKH);
    addValuesForSectionToMap(nodeMap, "earlyFrenchImmersion", "Early French Immersion", "10", includeKH);
    addValuesForSectionToMap(nodeMap, "lateFrenchImmersion", "Late French Immersion", "20", includeKH);
    addValuesForSectionToMap(nodeMap, "csf", "Programme Francophone", "30", includeKH);
    addValuesForSectionToMap(nodeMap, "allFrenchPrograms", "All French Programs", "40", includeKH);

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH){
    nodeMap.put(sectionPrefix + "Heading", new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, false, includeKH));
    nodeMap.put(sectionPrefix + "SchoolAged", new GradeHeadcountChildNode("School-Aged", FALSE, sequencePrefix + "1", false, true, false, includeKH));
    nodeMap.put(sectionPrefix + "Adult", new GradeHeadcountChildNode("Adult", FALSE, sequencePrefix + "2", false, true, false, includeKH));
  }

  public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, FrenchHeadcountResult frenchHeadcountResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(frenchHeadcountResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, GRADEVALUE, frenchHeadcountResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchHeading")).setValueForGrade(code, frenchHeadcountResult.getTotalCoreFrench());
    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchSchoolAged")).setValueForGrade(code, frenchHeadcountResult.getSchoolAgedCoreFrench());
    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchAdult")).setValueForGrade(code, frenchHeadcountResult.getAdultCoreFrench());

    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionHeading")).setValueForGrade(code, frenchHeadcountResult.getTotalEarlyFrench());
    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionSchoolAged")).setValueForGrade(code, frenchHeadcountResult.getSchoolAgedEarlyFrench());
    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionAdult")).setValueForGrade(code, frenchHeadcountResult.getAdultEarlyFrench());

    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionHeading")).setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());
    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionSchoolAged")).setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());
    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionAdult")).setValueForGrade(code, frenchHeadcountResult.getTotalLateFrench());

    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsHeading")).setValueForGrade(code, frenchHeadcountResult.getTotalTotals());
    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsSchoolAged")).setValueForGrade(code, frenchHeadcountResult.getSchoolAgedTotals());
    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsAdult")).setValueForGrade(code, frenchHeadcountResult.getAdultTotals());
  }

  private void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, CsfFrenchHeadcountResult frenchHeadcountResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(frenchHeadcountResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, GRADEVALUE, frenchHeadcountResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("csfHeading")).setValueForGrade(code, frenchHeadcountResult.getTotalFrancophone());
    ((GradeHeadcountChildNode)nodeMap.get("csfSchoolAged")).setValueForGrade(code, frenchHeadcountResult.getSchoolAgedFrancophone());
    ((GradeHeadcountChildNode)nodeMap.get("csfAdult")).setValueForGrade(code, frenchHeadcountResult.getAdultFrancophone());
  }

  public void setRowValues(Map<String, HeadcountChildNode> nodeMap, FrenchCombinedHeadcountResult frenchCombinedHeadcountResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(frenchCombinedHeadcountResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, GRADEVALUE, frenchCombinedHeadcountResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchHeading")).setValueForGrade(code, frenchCombinedHeadcountResult.getTotalCoreFrench());
    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchSchoolAged")).setValueForGrade(code, frenchCombinedHeadcountResult.getSchoolAgedCoreFrench());
    ((GradeHeadcountChildNode)nodeMap.get("coreFrenchAdult")).setValueForGrade(code, frenchCombinedHeadcountResult.getAdultCoreFrench());

    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionHeading")).setValueForGrade(code, frenchCombinedHeadcountResult.getTotalEarlyFrench());
    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionSchoolAged")).setValueForGrade(code, frenchCombinedHeadcountResult.getSchoolAgedEarlyFrench());
    ((GradeHeadcountChildNode)nodeMap.get("earlyFrenchImmersionAdult")).setValueForGrade(code, frenchCombinedHeadcountResult.getAdultEarlyFrench());

    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionHeading")).setValueForGrade(code, frenchCombinedHeadcountResult.getTotalLateFrench());
    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionSchoolAged")).setValueForGrade(code, frenchCombinedHeadcountResult.getSchoolAgedLateFrench());
    ((GradeHeadcountChildNode)nodeMap.get("lateFrenchImmersionAdult")).setValueForGrade(code, frenchCombinedHeadcountResult.getAdultLateFrench());

    ((GradeHeadcountChildNode)nodeMap.get("csfHeading")).setValueForGrade(code, frenchCombinedHeadcountResult.getTotalFrancophone());
    ((GradeHeadcountChildNode)nodeMap.get("csfSchoolAged")).setValueForGrade(code, frenchCombinedHeadcountResult.getSchoolAgedFrancophone());
    ((GradeHeadcountChildNode)nodeMap.get("csfAdult")).setValueForGrade(code, frenchCombinedHeadcountResult.getAdultFrancophone());

    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsHeading")).setValueForGrade(code, frenchCombinedHeadcountResult.getTotalTotals());
    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsSchoolAged")).setValueForGrade(code, frenchCombinedHeadcountResult.getSchoolAgedTotals());
    ((GradeHeadcountChildNode)nodeMap.get("allFrenchProgramsAdult")).setValueForGrade(code, frenchCombinedHeadcountResult.getAdultTotals());
  }

}
