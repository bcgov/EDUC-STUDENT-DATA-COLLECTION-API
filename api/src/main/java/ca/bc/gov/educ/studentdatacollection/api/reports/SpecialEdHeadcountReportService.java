package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SpecialEdHeadcountResult;
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
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class SpecialEdHeadcountReportService extends BaseReportGenerationService<SpecialEdHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private JasperReport specialEdHeadcountReport;

  public SpecialEdHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/specialEdHeadcounts.jrxml");
      specialEdHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateSchoolSpecialEdHeadcountReport(UUID sdcSchoolCollectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));

      var programList = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(programList, sdcSchoolCollectionEntity), specialEdHeadcountReport, SchoolReportTypeCode.SPECIAL_EDUCATION_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for inclusive education programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for inclusive education programs :: " + e.getMessage());
    }
  }


  public DownloadableReportResponse generateDistrictSpecialEdHeadcountReport(UUID sdcDistrictCollectionID){
    try {
      Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
      SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID" + sdcDistrictCollectionID));

      var programList = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
      return generateJasperReport(convertToReportJSONStringDistrict(programList, sdcDistrictCollectionEntity), specialEdHeadcountReport, DistrictReportTypeCode.DIS_SPECIAL_EDUCATION_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for district inclusive education programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for district inclusive education programs :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    nodeMap.put("level1Heading", new GradeHeadcountChildNode("Level 1", "true", "00", false, false, false, includeKH));
    nodeMap.put("level1A", new GradeHeadcountChildNode("A - Physically Dependent", FALSE, "01", false, false, false, includeKH));
    nodeMap.put("level1B", new GradeHeadcountChildNode("B - Deafblind", FALSE, "02", false, false, false, includeKH));

    nodeMap.put("level2Heading", new GradeHeadcountChildNode("Level 2", "true", "10", false, false, false, includeKH));
    nodeMap.put("level2C", new GradeHeadcountChildNode("C - Moderate to Profound Intellectual Disability", FALSE, "11", false, false, false, includeKH));
    nodeMap.put("level2D", new GradeHeadcountChildNode("D - Physical Disability or Chronic Health Impairment", FALSE, "12", false, false, false, includeKH));
    nodeMap.put("level2E", new GradeHeadcountChildNode("E - Visual Impairment", FALSE, "13", false, false, false, includeKH));
    nodeMap.put("level2F", new GradeHeadcountChildNode("F - Deaf or Hard of Hearing", FALSE, "14", false, false, false, includeKH));
    nodeMap.put("level2G", new GradeHeadcountChildNode("G - Autism Spectrum Disorder", FALSE, "15", false, false, false, includeKH));

    nodeMap.put("level3Heading", new GradeHeadcountChildNode("Level 3", "true", "20", false, false, false, includeKH));
    nodeMap.put("level3H", new GradeHeadcountChildNode("H - Intensive Behaviour Interventions or Serious Mental Illness", FALSE, "21", false, false, false, includeKH));

    nodeMap.put("otherHeading", new GradeHeadcountChildNode("Other", "true", "30", false, false, false, includeKH));
    nodeMap.put("otherK", new GradeHeadcountChildNode("K - Mild Intellectual Disability", FALSE, "31", false, false, false, includeKH));
    nodeMap.put("otherP", new GradeHeadcountChildNode("P - Gifted", FALSE, "32", false, false, false, includeKH));
    nodeMap.put("otherQ", new GradeHeadcountChildNode("Q - Learning Disability", FALSE, "33", false, false, false, includeKH));
    nodeMap.put("otherR", new GradeHeadcountChildNode("R - Moderate Behaviour Support/Mental Illness", FALSE, "34", false, false, false, includeKH));

    nodeMap.put("allHeading", new GradeHeadcountChildNode("All Levels & Categories", "true", "40", false, false, false, includeKH));
    return nodeMap;
  }

  public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, SpecialEdHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("level1Heading")).setValueForGrade(code, gradeResult.getLevelOnes());
    ((GradeHeadcountChildNode)nodeMap.get("level1A")).setValueForGrade(code, gradeResult.getSpecialEdACodes());
    ((GradeHeadcountChildNode)nodeMap.get("level1B")).setValueForGrade(code, gradeResult.getSpecialEdBCodes());

    ((GradeHeadcountChildNode)nodeMap.get("level2Heading")).setValueForGrade(code, gradeResult.getLevelTwos());
    ((GradeHeadcountChildNode)nodeMap.get("level2C")).setValueForGrade(code, gradeResult.getSpecialEdCCodes());
    ((GradeHeadcountChildNode)nodeMap.get("level2D")).setValueForGrade(code, gradeResult.getSpecialEdDCodes());
    ((GradeHeadcountChildNode)nodeMap.get("level2E")).setValueForGrade(code, gradeResult.getSpecialEdECodes());
    ((GradeHeadcountChildNode)nodeMap.get("level2F")).setValueForGrade(code, gradeResult.getSpecialEdFCodes());
    ((GradeHeadcountChildNode)nodeMap.get("level2G")).setValueForGrade(code, gradeResult.getSpecialEdGCodes());

    ((GradeHeadcountChildNode)nodeMap.get("level3Heading")).setValueForGrade(code, gradeResult.getLevelThrees());
    ((GradeHeadcountChildNode)nodeMap.get("level3H")).setValueForGrade(code, gradeResult.getSpecialEdHCodes());

    ((GradeHeadcountChildNode)nodeMap.get("otherHeading")).setValueForGrade(code, gradeResult.getOtherLevels());
    ((GradeHeadcountChildNode)nodeMap.get("otherK")).setValueForGrade(code, gradeResult.getSpecialEdKCodes());
    ((GradeHeadcountChildNode)nodeMap.get("otherP")).setValueForGrade(code, gradeResult.getSpecialEdPCodes());
    ((GradeHeadcountChildNode)nodeMap.get("otherQ")).setValueForGrade(code, gradeResult.getSpecialEdQCodes());
    ((GradeHeadcountChildNode)nodeMap.get("otherR")).setValueForGrade(code, gradeResult.getSpecialEdRCodes());

    ((GradeHeadcountChildNode)nodeMap.get("allHeading")).setValueForGrade(code, gradeResult.getAllLevels());
  }

}
