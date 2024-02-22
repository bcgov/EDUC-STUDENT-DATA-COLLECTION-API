package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.GradeEnrollementFTENode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.GradeEnrollementFTEReportGradesNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.GradeEnrollementFTEReportNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.util.concurrent.AtomicDouble;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class ReportGenerationService {

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private JasperReport gradeEnrollmentFTEReport;
  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  public ReportGenerationService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
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
      InputStream input = getClass().getResourceAsStream("/gradeEnrollmentFTEReport.jrxml");
      gradeEnrollmentFTEReport = JasperCompileManager.compileReport(input);
    } catch (JRException e) {
      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
    }
  }

  public byte[] generateGradeEnrollementFTEReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

      var gradeEnrollmentList = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionEntity.getSdcSchoolCollectionID());

      Map<String, Object> params = new HashMap<>();
      params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
      params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
      params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
      params.put(JRParameter.REPORT_LOCALE, Locale.US);

      InputStream targetStream = new ByteArrayInputStream(convertToReportJSONString(gradeEnrollmentList).getBytes());
      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);

      JasperPrint jasperPrint = JasperFillManager.fillReport(gradeEnrollmentFTEReport, params);
      return JasperExportManager.exportReportToPdf(jasperPrint);
    } catch (Exception e) {
      log.info("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
      throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  private String convertToReportJSONString(List<EnrollmentHeadcountResult> results) throws JsonProcessingException {
    GradeEnrollementFTENode mainNode = new GradeEnrollementFTENode();
    GradeEnrollementFTEReportNode reportNode = new GradeEnrollementFTEReportNode();
    reportNode.setReportGeneratedDate(LocalDate.now().format(formatter));
    reportNode.setDistrictNumberAndName("Marco Test");
    reportNode.setCollectionNameAndYear("September 2023 Collections");
    reportNode.setSchoolMincodeAndName("085 - Vancouver Island");
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
      GradeEnrollementFTEReportGradesNode grade = getGradeEnrollementFTEReportGradesNode(hcResult);
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
      grade.setTotalSchoolAgedFTETotal(totalSchoolAgedFTETotal.toString());
      grade.setTotalAdultsHeadcount(totalAdultHeadcount.toString());
      grade.setTotalAdultsEligibleForFTE(totalAdultEligibleForFTE.toString());
      grade.setTotalAdultsFTETotal(totalAdultFTETotal.toString());
      grade.setTotalAllStudentsHeadcount(totalAllStudentHeadcount.toString());
      grade.setTotalAllStudentsEligibleForFTE(totalAllStudentEligibleForFTE.toString());
      grade.setTotalAllStudentsFTETotal(totalAllStudentFTETotal.toString());
    });

    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  private GradeEnrollementFTEReportGradesNode getGradeEnrollementFTEReportGradesNode(EnrollmentHeadcountResult hcResult) {
    GradeEnrollementFTEReportGradesNode grade = new GradeEnrollementFTEReportGradesNode();
    grade.setCode(hcResult.getEnrolledGradeCode());
    grade.setSchoolAgedHeadcount(hcResult.getSchoolAgedHeadcount());
    grade.setSchoolAgedEligibleForFTE(hcResult.getSchoolAgedEligibleForFte());
    grade.setSchoolAgedFTETotal(hcResult.getSchoolAgedFteTotal());
    grade.setAdultHeadcount(hcResult.getAdultHeadcount());
    grade.setAdultEligibleForFTE(hcResult.getAdultEligibleForFte());
    grade.setAdultFTETotal(hcResult.getAdultFteTotal());
    grade.setAllStudentHeadcount(hcResult.getTotalHeadcount());
    grade.setAllStudentEligibleForFTE(hcResult.getTotalEligibleForFte());
    grade.setAllStudentFTETotal(hcResult.getTotalFteTotal());
    return grade;
  }

}
