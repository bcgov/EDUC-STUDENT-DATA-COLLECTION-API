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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndigenousHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountReportNode;
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

@Service
@Slf4j
public class IndigenousHeadcountReportService extends BaseReportGenerationService<IndigenousHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport indigenousHeadcountReport;

  public IndigenousHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/indigenousHeadcounts.jrxml");
      indigenousHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateIndigenousHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var headcountsList = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToReportJSONString(headcountsList, sdcSchoolCollectionEntity), indigenousHeadcountReport, ReportTypeCode.INDIGENOUS_HEADCOUNT);
    } catch (JsonProcessingException e) {
      log.info("Exception occurred while writing PDF report for indigenous programs :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for indigenous programs :: " + e.getMessage());
    }
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "indigenousLanguage", "Indigenous Language & Culture", "00");
    addValuesForSectionToMap(nodeMap, "indigenousSupport", "Indigenous Support Services", "10");
    addValuesForSectionToMap(nodeMap, "otherApproved", "Other Approved Indigenous Programs", "20");
    addValuesForSectionToMap(nodeMap, "all", "All Indigenous Support Programs", "30");

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix){
    nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false));
  }

  public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, IndigenousHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    nodeMap.get("indigenousLanguageHeading").setValueForGrade(code, gradeResult.getIndigenousLanguageTotal());
    nodeMap.get("indigenousSupportHeading").setValueForGrade(code, gradeResult.getIndigenousSupportTotal());
    nodeMap.get("otherApprovedHeading").setValueForGrade(code, gradeResult.getOtherProgramTotal());
    nodeMap.get("allHeading").setValueForGrade(code, gradeResult.getAllSupportProgramTotal());
  }

}