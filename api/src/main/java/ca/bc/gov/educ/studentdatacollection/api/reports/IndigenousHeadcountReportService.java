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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndigenousHeadcountResult;
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
public class IndigenousHeadcountReportService extends BaseReportGenerationService<IndigenousHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
  private JasperReport indigenousHeadcountReport;

  public IndigenousHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/indigenousHeadcounts.jrxml");
      indigenousHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateSchoolIndigenousHeadcountReport(UUID sdcSchoolCollectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional = sdcSchoolCollectionRepository.findById(sdcSchoolCollectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));

      var headcountsList = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(headcountsList, sdcSchoolCollectionEntity), indigenousHeadcountReport, SchoolReportTypeCode.INDIGENOUS_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for indigenous programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for indigenous programs :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateDistrictIndigenousHeadcountReport(UUID sdcDistrictCollectionID){
    try {
      Optional<SdcDistrictCollectionEntity> sdcDistrictCollectionEntityOptional = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
      SdcDistrictCollectionEntity sdcDistrictCollectionEntity = sdcDistrictCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcDistrictCollectionEntity.class, "sdcDistrictCollectionID " + sdcDistrictCollectionID));

      var programList = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
      return generateJasperReport(convertToReportJSONStringDistrict(programList, sdcDistrictCollectionEntity), indigenousHeadcountReport, DistrictReportTypeCode.DIS_INDIGENOUS_HEADCOUNT.getCode());
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for district indigenous programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for district indigenous programs :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "indigenousLanguage", "Indigenous Language & Culture", "00", includeKH);
    addValuesForSectionToMap(nodeMap, "indigenousSupport", "Indigenous Support Services", "10", includeKH);
    addValuesForSectionToMap(nodeMap, "otherApproved", "Other Approved Indigenous Programs", "20", includeKH);
    addValuesForSectionToMap(nodeMap, "all", "All Indigenous Support Programs", "30", includeKH);

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH){
    nodeMap.put(sectionPrefix + "Heading", new GradeHeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, false, false, includeKH));
  }

  public void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, IndigenousHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    ((GradeHeadcountChildNode)nodeMap.get("indigenousLanguageHeading")).setValueForGrade(code, gradeResult.getIndigenousLanguageTotal());
    ((GradeHeadcountChildNode)nodeMap.get("indigenousSupportHeading")).setValueForGrade(code, gradeResult.getIndigenousSupportTotal());
    ((GradeHeadcountChildNode)nodeMap.get("otherApprovedHeading")).setValueForGrade(code, gradeResult.getOtherProgramTotal());
    ((GradeHeadcountChildNode)nodeMap.get("allHeading")).setValueForGrade(code, gradeResult.getAllSupportProgramTotal());
  }

}
