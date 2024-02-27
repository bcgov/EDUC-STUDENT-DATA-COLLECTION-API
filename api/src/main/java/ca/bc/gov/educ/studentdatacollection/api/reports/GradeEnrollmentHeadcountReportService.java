package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.gradeenrollmentheadcount.GradeEnrollmentHeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.gradeenrollmentheadcount.GradeEnrollmentHeadcountReportGradesNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.gradeenrollmentheadcount.GradeEnrollmentHeadcountReportNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.util.concurrent.AtomicDouble;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class GradeEnrollmentHeadcountReportService extends BaseReportGenerationService{

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport gradeEnrollmentHeadcountReport;
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
  private static final String DOUBLE_FORMAT = "%,.4f";

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
      InputStream inputGradeHeadcount = getClass().getResourceAsStream("/reports/gradeEnrollmentFTEReport.jrxml");
      gradeEnrollmentHeadcountReport = JasperCompileManager.compileReport(inputGradeHeadcount);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public String generateGradeEnrollmentHeadcountReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var gradeEnrollmentList = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());

      return generateJasperReport(convertToGradeEnrollmentReportJSONString(gradeEnrollmentList, sdcSchoolCollectionEntity), gradeEnrollmentHeadcountReport);
    } catch (JsonProcessingException e) {
      log.info("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  private String convertToGradeEnrollmentReportJSONString(List<EnrollmentHeadcountResult> results, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    GradeEnrollmentHeadcountNode mainNode = new GradeEnrollmentHeadcountNode();
    GradeEnrollmentHeadcountReportNode reportNode = new GradeEnrollmentHeadcountReportNode();
    setReportTombstoneValues(sdcSchoolCollection, reportNode);
    reportNode.setGrades(new ArrayList<>());

    AtomicInteger totalSchoolAgedHeadcount = new AtomicInteger(0);
    AtomicInteger totalSchoolAgedEligibleForFTE = new AtomicInteger(0);
    AtomicDouble totalSchoolAgedFTETotal = new AtomicDouble(0);
    AtomicInteger totalAdultHeadcount = new AtomicInteger(0);
    AtomicInteger totalAdultEligibleForFTE = new AtomicInteger(0);
    AtomicDouble totalAdultFTETotal = new AtomicDouble(0);
    AtomicInteger totalAllStudentHeadcount = new AtomicInteger(0);
    AtomicInteger totalAllStudentEligibleForFTE = new AtomicInteger(0);
    AtomicDouble totalAllStudentFTETotal = new AtomicDouble(0);

    results.forEach(hcResult -> {
      GradeEnrollmentHeadcountReportGradesNode grade = getGradeEnrollmentFTEReportGradesNode(hcResult);
      reportNode.getGrades().add(grade);

      totalSchoolAgedHeadcount.addAndGet(Integer.valueOf(grade.getSchoolAgedHeadcount()));
      totalSchoolAgedEligibleForFTE.addAndGet(Integer.valueOf(grade.getSchoolAgedEligibleForFTE()));
      totalSchoolAgedFTETotal.addAndGet(Double.valueOf(grade.getSchoolAgedFTETotal()));
      totalAdultHeadcount.addAndGet(Integer.valueOf(grade.getAdultHeadcount()));
      totalAdultEligibleForFTE.addAndGet(Integer.valueOf(grade.getAdultEligibleForFTE()));
      totalAdultFTETotal.addAndGet(Double.valueOf(grade.getAdultFTETotal()));
      totalAllStudentHeadcount.addAndGet(Integer.valueOf(grade.getAllStudentHeadcount()));
      totalAllStudentEligibleForFTE.addAndGet(Integer.valueOf(grade.getAllStudentEligibleForFTE()));
      totalAllStudentFTETotal.addAndGet(Double.valueOf(grade.getAllStudentFTETotal()));
    });

    reportNode.getGrades().forEach(grade -> {
      grade.setTotalCountsCode("Total");
      grade.setTotalSchoolAgedHeadcount(totalSchoolAgedHeadcount.toString());
      grade.setTotalSchoolAgedEligibleForFTE(totalSchoolAgedEligibleForFTE.toString());
      grade.setTotalSchoolAgedFTETotal(String.format(DOUBLE_FORMAT, totalSchoolAgedFTETotal.doubleValue()));
      grade.setTotalAdultsHeadcount(totalAdultHeadcount.toString());
      grade.setTotalAdultsEligibleForFTE(totalAdultEligibleForFTE.toString());
      grade.setTotalAdultsFTETotal(String.format(DOUBLE_FORMAT, totalAdultFTETotal.doubleValue()));
      grade.setTotalAllStudentsHeadcount(totalAllStudentHeadcount.toString());
      grade.setTotalAllStudentsEligibleForFTE(totalAllStudentEligibleForFTE.toString());
      grade.setTotalAllStudentsFTETotal(String.format(DOUBLE_FORMAT, totalAllStudentFTETotal.doubleValue()));
    });

    var allGradeCodes = SchoolGradeCodes.getAllSchoolGrades();
    reportNode.setGrades(reportNode.getGrades().stream().sorted((o1, o2)->Integer.compare(allGradeCodes.indexOf(o1.getCode()), allGradeCodes.indexOf(o2.getCode()))).toList());

    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private GradeEnrollmentHeadcountReportGradesNode getGradeEnrollmentFTEReportGradesNode(EnrollmentHeadcountResult hcResult) {
    GradeEnrollmentHeadcountReportGradesNode grade = new GradeEnrollmentHeadcountReportGradesNode();
    grade.setCode(hcResult.getEnrolledGradeCode());
    grade.setSchoolAgedHeadcount(hcResult.getSchoolAgedHeadcount());
    grade.setSchoolAgedEligibleForFTE(hcResult.getSchoolAgedEligibleForFte());
    grade.setSchoolAgedFTETotal(String.format(DOUBLE_FORMAT, Double.valueOf(hcResult.getSchoolAgedFteTotal())));
    grade.setAdultHeadcount(hcResult.getAdultHeadcount());
    grade.setAdultEligibleForFTE(hcResult.getAdultEligibleForFte());
    grade.setAdultFTETotal(String.format(DOUBLE_FORMAT, Double.valueOf(hcResult.getAdultFteTotal())));
    grade.setAllStudentHeadcount(hcResult.getTotalHeadcount());
    grade.setAllStudentEligibleForFTE(hcResult.getTotalEligibleForFte());
    grade.setAllStudentFTETotal(String.format(DOUBLE_FORMAT, Double.valueOf(hcResult.getTotalFteTotal())));
    return grade;
  }
}
