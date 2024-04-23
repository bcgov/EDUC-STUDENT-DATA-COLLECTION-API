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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
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
public class GradeEnrollmentHeadcountReportService extends BaseReportGenerationService<EnrollmentHeadcountResult>{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport gradeEnrollmentHeadcountReport;
  private static final String DOUBLE_FORMAT = "%,.4f";
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  public GradeEnrollmentHeadcountReportService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, RestUtils restUtils) {
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
      InputStream inputHeadcount = getClass().getResourceAsStream("/reports/gradeEnrollmentHeadcounts.jrxml");
      gradeEnrollmentHeadcountReport = JasperCompileManager.compileReport(inputHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public DownloadableReportResponse generateGradeEnrollmentHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var gradeEnrollmentList = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
      return generateJasperReport(convertToGradeEnrollmentReportJSONString(gradeEnrollmentList, sdcSchoolCollectionEntity), gradeEnrollmentHeadcountReport, ReportTypeCode.GRADE_ENROLLMENT_HEADCOUNT);
    } catch (JsonProcessingException e) {
      log.error("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  private String convertToGradeEnrollmentReportJSONString(List<EnrollmentHeadcountResult> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    var school = setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap(isIndependentSchool(school));

    mappedResults.forEach(careerHeadcountResult -> setValueForGrade(nodeMap, careerHeadcountResult));

    nodeMap.get("underSchoolAgedHeading").setAllValuesToNull();
    nodeMap.get("schoolAgedHeading").setAllValuesToNull();
    nodeMap.get("adultHeading").setAllValuesToNull();
    nodeMap.get("allHeading").setAllValuesToNull();

    reportNode.setPrograms(nodeMap.values().stream().sorted((o1, o2)->o1.getSequence().compareTo(o2.getSequence())).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  public HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH){
    HashMap<String, HeadcountChildNode> nodeMap = new HashMap<>();
    addValuesForSectionToMap(nodeMap, "underSchoolAged", "Preschool Aged", "00", includeKH);
    addValuesForSectionToMap(nodeMap, "schoolAged", "School Aged", "10", includeKH);
    addValuesForSectionToMap(nodeMap, "adult", "Adult", "20", includeKH);
    addValuesForSectionToMap(nodeMap, "all", "All Students", "30", includeKH);

    return nodeMap;
  }

  private void addValuesForSectionToMap(HashMap<String, HeadcountChildNode> nodeMap, String sectionPrefix, String sectionTitle, String sequencePrefix, boolean includeKH){
    nodeMap.put(sectionPrefix + "Heading", new HeadcountChildNode(sectionTitle, "true", sequencePrefix + "0", false, true, true, includeKH));
    nodeMap.put(sectionPrefix + "Headcount", new HeadcountChildNode("Headcount", FALSE, sequencePrefix + "1", false, true, true, includeKH));
    nodeMap.put(sectionPrefix + "EligibleForFTE", new HeadcountChildNode("Eligible For FTE", FALSE, sequencePrefix + "2", false, true, true, includeKH));
    nodeMap.put(sectionPrefix + "FTETotal", new HeadcountChildNode("FTE Total", FALSE, sequencePrefix + "3", true, true, true, includeKH));
  }

  public void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, EnrollmentHeadcountResult gradeResult){
    Optional<SchoolGradeCodes> optionalCode = SchoolGradeCodes.findByValue(gradeResult.getEnrolledGradeCode());
    var code = optionalCode.orElseThrow(() ->
            new EntityNotFoundException(SchoolGradeCodes.class, "Grade Value", gradeResult.getEnrolledGradeCode()));

    nodeMap.get("underSchoolAgedHeadcount").setValueForGrade(code, gradeResult.getUnderSchoolAgedHeadcount());
    nodeMap.get("underSchoolAgedEligibleForFTE").setValueForGrade(code, gradeResult.getUnderSchoolAgedEligibleForFte());
    nodeMap.get("underSchoolAgedFTETotal").setValueForGrade(code, String.format(DOUBLE_FORMAT, Double.valueOf(gradeResult.getUnderSchoolAgedFteTotal())));

    nodeMap.get("schoolAgedHeadcount").setValueForGrade(code, gradeResult.getSchoolAgedHeadcount());
    nodeMap.get("schoolAgedEligibleForFTE").setValueForGrade(code, gradeResult.getSchoolAgedEligibleForFte());
    nodeMap.get("schoolAgedFTETotal").setValueForGrade(code, String.format(DOUBLE_FORMAT, Double.valueOf(gradeResult.getSchoolAgedFteTotal())));

    nodeMap.get("adultHeadcount").setValueForGrade(code, gradeResult.getAdultHeadcount());
    nodeMap.get("adultEligibleForFTE").setValueForGrade(code, gradeResult.getAdultEligibleForFte());
    nodeMap.get("adultFTETotal").setValueForGrade(code, String.format(DOUBLE_FORMAT, Double.valueOf(gradeResult.getAdultFteTotal())));

    nodeMap.get("allHeadcount").setValueForGrade(code, gradeResult.getTotalHeadcount());
    nodeMap.get("allEligibleForFTE").setValueForGrade(code, gradeResult.getTotalEligibleForFte());
    nodeMap.get("allFTETotal").setValueForGrade(code, String.format(DOUBLE_FORMAT, Double.valueOf(gradeResult.getTotalFteTotal())));
  }

}
