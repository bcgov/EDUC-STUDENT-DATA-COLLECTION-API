package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Slf4j
public abstract class BaseReportGenerationService<T> {

  private final RestUtils restUtils;
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

  protected static final String FALSE = "false";

  private ObjectWriter objectWriter = new ObjectMapper().writer().withDefaultPrettyPrinter();

  protected BaseReportGenerationService(RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  protected DownloadableReportResponse generateJasperReport(String reportJSON, JasperReport jasperReport, ReportTypeCode reportTypeCode){
    try{
      var params = getJasperParams();
      InputStream targetStream = new ByteArrayInputStream(reportJSON.getBytes());
      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);

      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
      var downloadableReport = new DownloadableReportResponse();
      downloadableReport.setReportType(reportTypeCode.getCode());
      downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(jasperPrint)));
      return downloadableReport;
    } catch (JRException e) {
       log.error("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
       throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  protected abstract HashMap<String, HeadcountChildNode> generateNodeMap(boolean includeKH);

  protected abstract void setValueForGrade(HashMap<String, HeadcountChildNode> nodeMap, T gradeResult);

  protected String convertToReportJSONString(List<T> mappedResults, SdcSchoolCollectionEntity sdcSchoolCollection) throws JsonProcessingException {
    HeadcountNode mainNode = new HeadcountNode();
    HeadcountReportNode reportNode = new HeadcountReportNode();
    var school = setReportTombstoneValues(sdcSchoolCollection, reportNode);

    var nodeMap = generateNodeMap(isIndependentSchool(school));

    mappedResults.forEach(headcountResult -> setValueForGrade(nodeMap, headcountResult));

    reportNode.setPrograms(nodeMap.values().stream().sorted(Comparator.comparing(o -> Integer.parseInt(o.getSequence()))).toList());
    mainNode.setReport(reportNode);
    return objectWriter.writeValueAsString(mainNode);
  }

  protected District validateAndReturnDistrict(School school){
    var district = restUtils.getDistrictByDistrictID(school.getDistrictId());
    if(district.isEmpty()){
      log.error("District could not be found while writing PDF report for grade enrollment :: " + school.getDistrictId());
      throw new EntityNotFoundException(District.class, "District could not be found while writing PDF report for grade enrollment :: ", school.getDistrictId());
    }

    return district.get();
  }

  protected School validateAndReturnSchool(SdcSchoolCollectionEntity sdcSchoolCollection){
    var school = restUtils.getSchoolBySchoolID(sdcSchoolCollection.getSchoolID().toString());
    if(school.isEmpty()){
      log.error("School could not be found while writing PDF report for grade enrollment :: " + sdcSchoolCollection.getSchoolID().toString());
      throw new EntityNotFoundException(School.class, "School could not be found while writing PDF report for grade enrollment :: ", sdcSchoolCollection.getSchoolID().toString());
    }

    return school.get();
  }

  protected School setReportTombstoneValues(SdcSchoolCollectionEntity sdcSchoolCollection, HeadcountReportNode reportNode){
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

  protected boolean isIndependentSchool(School school){
    return school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDEPEND.getCode()) || school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.INDP_FNS.getCode());
  }

  protected Map<String, Object> getJasperParams(){
    Map<String, Object> params = new HashMap<>();
    params.put(JsonQueryExecuterFactory.JSON_DATE_PATTERN, "yyyy-MM-dd");
    params.put(JsonQueryExecuterFactory.JSON_NUMBER_PATTERN, "#,##0.##");
    params.put(JsonQueryExecuterFactory.JSON_LOCALE, Locale.ENGLISH);
    params.put(JRParameter.REPORT_LOCALE, Locale.US);
    return params;
  }

}
