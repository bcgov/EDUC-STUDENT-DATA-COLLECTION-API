package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class ReportGenerationService {

  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
//  private JasperReport gradeEnrollmentFTEReport;
//  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  public ReportGenerationService(SdcSchoolCollectionRepository sdcSchoolCollectionRepository) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
  }

//  @PostConstruct
//  public void init() {
//    ApplicationProperties.bgTask.execute(this::initialize);
//  }
//
//  private void initialize() {
//    this.compileJasperReports();
//  }
//
//  private void compileJasperReports(){
//    try {
//      InputStream input = getClass().getResourceAsStream("/cherry.jrxml");
//      gradeEnrollmentFTEReport = JasperCompileManager.compileReport(input);
//    } catch (JRException e) {
//      throw new StudentDataCollectionAPIRuntimeException("Compiling Jasper reports has failed :: " + e.getMessage());
//    }
//  }

  public byte[] generateGradeEnrollementFTEReport(UUID collectionID){
    try {
      Optional<SdcSchoolCollectionEntity> sdcSchoolCollectionEntityOptional =  sdcSchoolCollectionRepository.findById(collectionID);
      SdcSchoolCollectionEntity sdcSchoolCollectionEntity = sdcSchoolCollectionEntityOptional.orElseThrow(() ->
              new EntityNotFoundException(SdcSchoolCollectionEntity.class, "Collection by Id", collectionID.toString()));

//      Map<String, Object> params = new HashMap<>();
//      params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
//      params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
//      params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
//      params.put(JRParameter.REPORT_LOCALE, Locale.US);
//
////      String json = "{\"report\": {  \"collectionNameAndYear\": \"September 2023 Collection\",  \"reportGeneratedDate\": \"Report Date: 2023-10-03\",  \"districtNumberAndName\": \"085 - Vancouver Island\",  \"schoolMincodeAndName\": \"08585023 - Eagle View Elementary School\",  \"grades\": [{  \"code\": \"KF\",  \"schoolAgedHeadcount\": \"100\",  \"schoolAgedEligibleForFTE\": \"0\",  \"schoolAgedFTETotal\": \"100.0000\",  \"adultHeadcount\": \"100\",  \"adultEligibleForFTE\": \"0\",  \"adultFTETotal\": \"100.0000\",  \"allStudentHeadcount\": \"90\",  \"allStudentEligibleForFTE\": \"10\",  \"allStudentFTETotal\": \"90.0000\"},{  \"code\": \"01\",  \"schoolAgedHeadcount\": \"100\",  \"schoolAgedEligibleForFTE\": \"50\",  \"schoolAgedFTETotal\": \"100.0000\",  \"adultHeadcount\": \"60\",  \"adultEligibleForFTE\": \"5\",  \"adultFTETotal\": \"150.0000\",  \"allStudentHeadcount\": \"100\",  \"allStudentEligibleForFTE\": \"0\",  \"allStudentFTETotal\": \"100.0000\",  \"totalCountsCode\": \"Total\",  \"totalSchoolAgedHeadcount\": \"300\"}  ]  }}";
//      String json = objectWriter.writeValueAsString(gradeEnrollementFTEReport);
//      InputStream targetStream = new ByteArrayInputStream(json.getBytes());
//      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);
//
//      JasperPrint jasperPrint = JasperFillManager.fillReport(gradeEnrollmentFTEReport, params);
//      JasperExportManager.exportReportToPdfFile(jasperPrint, "test.pdf");

      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(SdcSchoolCollectionMapper.mapper.toStructure(sdcSchoolCollectionEntity));
      oos.flush();
      return bos.toByteArray();
    } catch (Exception e) {
      log.info("ERROR is: " + e.getMessage());
    }
  }

}
