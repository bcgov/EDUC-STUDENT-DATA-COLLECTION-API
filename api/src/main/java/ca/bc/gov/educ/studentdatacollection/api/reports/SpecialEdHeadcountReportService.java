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
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SpecialEdHeadcountReportService extends BaseReportGenerationService<SpecialEdHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport specialEdHeadcountReport;

  public SpecialEdHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
      super(restUtils, sdcSchoolCollectionRepository);
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/specialEdHeadcounts.jrxml");
      specialEdHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateSpecialEdHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var programList = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(programList, sdcSchoolCollectionEntity), specialEdHeadcountReport, ReportTypeCode.SPECIAL_EDUCATION_HEADCOUNT);
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for inclusive education programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education programs :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    nodeMap.put("level1Heading", new HeadcountChildNode("Level 1", "true", "00", false, false, false, includeKH));
    nodeMap.put("level1A", new HeadcountChildNode("A - Physically Dependent", FALSE, "01", false, false, false, includeKH));
    nodeMap.put("level1B", new HeadcountChildNode("B - Deafblind", FALSE, "02", false, false, false, includeKH));

    nodeMap.put("level2Heading", new HeadcountChildNode("Level 2", "true", "10", false, false, false, includeKH));
    nodeMap.put("level2C", new HeadcountChildNode("C - Moderate to Profound Intellectual Disability", FALSE, "11", false, false, false, includeKH));
    nodeMap.put("level2D", new HeadcountChildNode("D - Physical Disability or Chronic Health Impairment", FALSE, "12", false, false, false, includeKH));
    nodeMap.put("level2E", new HeadcountChildNode("E - Visual Impairment", FALSE, "13", false, false, false, includeKH));
    nodeMap.put("level2F", new HeadcountChildNode("F - Deaf or Hard of Hearing", FALSE, "14", false, false, false, includeKH));
    nodeMap.put("level2G", new HeadcountChildNode("G - Autism Spectrum Disorder", FALSE, "15", false, false, false, includeKH));

    nodeMap.put("level3Heading", new HeadcountChildNode("Level 3", "true", "20", false, false, false, includeKH));
    nodeMap.put("level3H", new HeadcountChildNode("H - Intensive Behaviour Interventions or Serious Mental Illness", FALSE, "21", false, false, false, includeKH));

    nodeMap.put("otherHeading", new HeadcountChildNode("Other", "true", "30", false, false, false, includeKH));
    nodeMap.put("otherK", new HeadcountChildNode("K - Mild Intellectual Disability", FALSE, "31", false, false, false, includeKH));
    nodeMap.put("otherP", new HeadcountChildNode("P - Gifted", FALSE, "32", false, false, false, includeKH));
    nodeMap.put("otherQ", new HeadcountChildNode("Q - Learning Disability", FALSE, "33", false, false, false, includeKH));
    nodeMap.put("otherR", new HeadcountChildNode("R - Moderate Behaviour Support/Mental Illness", FALSE, "34", false, false, false, includeKH));

    nodeMap.put("allHeading", new HeadcountChildNode("All Levels & Categories", "true", "40", false, false, false, includeKH));
    return nodeMap;
  }

  public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, SpecialEdHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    nodeMap.get("level1Heading").setValueForGrade(code, gradeResult.getLevelOnes());
    nodeMap.get("level1A").setValueForGrade(code, gradeResult.getSpecialEdACodes());
    nodeMap.get("level1B").setValueForGrade(code, gradeResult.getSpecialEdBCodes());

    nodeMap.get("level2Heading").setValueForGrade(code, gradeResult.getLevelTwos());
    nodeMap.get("level2C").setValueForGrade(code, gradeResult.getSpecialEdCCodes());
    nodeMap.get("level2D").setValueForGrade(code, gradeResult.getSpecialEdDCodes());
    nodeMap.get("level2E").setValueForGrade(code, gradeResult.getSpecialEdECodes());
    nodeMap.get("level2F").setValueForGrade(code, gradeResult.getSpecialEdFCodes());
    nodeMap.get("level2G").setValueForGrade(code, gradeResult.getSpecialEdGCodes());

    nodeMap.get("level3Heading").setValueForGrade(code, gradeResult.getLevelThrees());
    nodeMap.get("level3H").setValueForGrade(code, gradeResult.getSpecialEdHCodes());

    nodeMap.get("otherHeading").setValueForGrade(code, gradeResult.getOtherLevels());
    nodeMap.get("otherK").setValueForGrade(code, gradeResult.getSpecialEdKCodes());
    nodeMap.get("otherP").setValueForGrade(code, gradeResult.getSpecialEdPCodes());
    nodeMap.get("otherQ").setValueForGrade(code, gradeResult.getSpecialEdQCodes());
    nodeMap.get("otherR").setValueForGrade(code, gradeResult.getSpecialEdRCodes());

    nodeMap.get("allHeading").setValueForGrade(code, gradeResult.getAllLevels());
  }

}
