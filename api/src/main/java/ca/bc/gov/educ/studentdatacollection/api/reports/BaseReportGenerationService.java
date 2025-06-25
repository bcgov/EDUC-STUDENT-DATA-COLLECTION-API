package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountReportNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public abstract class BaseReportGenerationService<T> {

  private final RestUtils restUtils;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  protected static final String FALSE = "false";
  protected static final NumberFormat numberFormat = NumberFormat.getInstance(Locale.US);

  protected ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  protected BaseReportGenerationService(RestUtils restUtils, SdcSchoolCollectionRepository sdcSchoolCollectionRepository) {
    this.restUtils = restUtils;
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
  }

  protected DownloadableReportResponse generateJasperReport(String reportJSON, JasperReport jasperReport, String schoolReportTypeCode){
    try{
      var params = getJasperParams();
      InputStream targetStream = new ByteArrayInputStream(reportJSON.getBytes());
      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);

      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
      var downloadableReport = new DownloadableReportResponse();
      downloadableReport.setReportType(schoolReportTypeCode);
      downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(jasperPrint)));
      return downloadableReport;
    } catch (JRException e) {
       log.error("Exception occurred while writing PDF report for grade enrolment :: " + e.getMessage());
       throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrolment :: " + e.getMessage());
    }
  }

  protected abstract HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH);

  protected abstract void setRowValues(HashMap<String, HeadcountChildNode> nodeMap, T gradeResult);

  protected String convertToReportJSONString(List<T> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    var school = setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap(isIndependentSchool(school));

    mappedResults.forEach(headcountResult -> setRowValues(nodeMap, headcountResult));

    reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  protected String convertToReportJSONStringDistrict(List<T> mappedResults, SdcDistrictCollectionEntity sdcDistrictCollection) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    setReportTombstoneValuesDis(sdcDistrictCollection, reportNode);

    var nodeMap = generateNodeMap(false);

    mappedResults.forEach(result -> setRowValues(nodeMap, result));

    reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  protected District validateAndReturnDistrict(SchoolTombstone schoolTombstone){
    var district = restUtils.getDistrictByDistrictID(schoolTombstone.getDistrictId());
    if(district.isEmpty()){
      log.error("District could not be found while writing PDF report for grade enrolment :: " + schoolTombstone.getDistrictId());
      throw new EntityNotFoundException(District.class, "District could not be found while writing PDF report for grade enrolment :: ", schoolTombstone.getDistrictId());
    }

    return district.get();
  }

  protected SchoolTombstone validateAndReturnSchool(SdcSchoolCollectionEntity sdcSchoolCollection){
    var school = restUtils.getSchoolBySchoolID(sdcSchoolCollection.getSchoolID().toString());
    if(school.isEmpty()){
      log.error("School could not be found while writing PDF report for grade enrolment :: " + sdcSchoolCollection.getSchoolID().toString());
      throw new EntityNotFoundException(SchoolTombstone.class, "School could not be found while writing PDF report for grade enrolment :: ", sdcSchoolCollection.getSchoolID().toString());
    }

    return school.get();
  }

  protected SchoolTombstone setReportTombstoneValues(SdcSchoolCollectionEntity sdcSchoolCollection, HeadcountReportNode reportNode){
    var school = validateAndReturnSchool(sdcSchoolCollection);
    var district = validateAndReturnDistrict(school);

    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setDistrictNumberAndName(district.getDistrictNumber() + " - " + district.getDisplayName());
    reportNode.setCollectionNameAndYear(StringUtils.capitalize(sdcSchoolCollection.getCollectionEntity().getCollectionTypeCode().toLowerCase()) + " " + sdcSchoolCollection.getCollectionEntity().getOpenDate().getYear() + " Collection");
    reportNode.setSchoolMincodeAndName(school.getMincode() + " - " + school.getDisplayName());

    if(isIndependentSchool(school)){
      reportNode.setShowKH("true");
    }
    return school;
  }

  protected void setReportTombstoneValuesDis(SdcDistrictCollectionEntity sdcDistrictCollection, HeadcountReportNode reportNode){
    var district = restUtils.getDistrictByDistrictID(sdcDistrictCollection.getDistrictID().toString());
    if(district.isEmpty()){
      log.error("District could not be found while writing tombstone for PDF report  :: " + sdcDistrictCollection.getDistrictID());
      throw new EntityNotFoundException(District.class, "District could not be found while writing tombstone for PDF report :: ", sdcDistrictCollection.getDistrictID().toString());
    }

    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setDistrictNumberAndName(district.get().getDistrictNumber() + " - " + district.get().getDisplayName());
    reportNode.setCollectionNameAndYear(StringUtils.capitalize(sdcDistrictCollection.getCollectionEntity().getCollectionTypeCode().toLowerCase()) + " " + sdcDistrictCollection.getCollectionEntity().getOpenDate().getYear() + " Collection");
    reportNode.setShowKH(FALSE);
  }

  protected boolean isIndependentSchool(SchoolTombstone schoolTombstone){
    return SchoolCategoryCodes.INDEPENDENTS.contains(schoolTombstone.getSchoolCategoryCode());
  }

  protected Map<String, Object> getJasperParams(){
    Map<String, Object> params = new HashMap<>();
    params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
    params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
    params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
    params.put(JRParameter.REPORT_LOCALE, Locale.US);
    return params;
  }

  public List<SchoolTombstone> getAllSchoolTombstones(UUID sdcDistrictCollectionID) {
    List<SdcSchoolCollectionEntity> allSchoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);

    return allSchoolCollections.stream()
            .map(schoolCollection -> restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString())
            .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollection.class, "SchoolID", schoolCollection.getSchoolID().toString())))
            .toList();
  }

  public List<SchoolTombstone> getAllSchoolTombstonesYouthPRP(UUID sdcDistrictCollectionID) {
    List<SdcSchoolCollectionEntity> allSchoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
    var prpAndYouthSchools = Arrays.asList(FacilityTypeCodes.SHORT_PRP.getCode(), FacilityTypeCodes.LONG_PRP.getCode(), FacilityTypeCodes.YOUTH.getCode());

    return allSchoolCollections.stream()
            .map(schoolCollection -> restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString())
            .orElseThrow(() -> new EntityNotFoundException(SdcSchoolCollection.class, "SchoolID", schoolCollection.getSchoolID().toString())))
            .filter(school -> prpAndYouthSchools.contains(school.getFacilityTypeCode()))
            .toList();
  }
}
