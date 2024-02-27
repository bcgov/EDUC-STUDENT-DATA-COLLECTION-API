package ca.bc.gov.educ.studentdatacollection.api.reports;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.District;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.BaseReportNode;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.query.JsonQueryExecuterFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
public class BaseReportGenerationService {

  private final RestUtils restUtils;
  DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
  protected static final String FALSE = "false";

  public BaseReportGenerationService(RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  protected String generateJasperReport(String reportJSON, JasperReport jasperReport){
    try{
      var params = getJasperParams();
      InputStream targetStream = new ByteArrayInputStream(reportJSON.getBytes());
      params.put(JsonQueryExecuterFactory.JSON_INPUT_STREAM, targetStream);

      JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, params);
      return Base64.getEncoder().encodeToString(JasperExportManager.exportReportToPdf(jasperPrint));
    } catch (JRException e) {
       log.info("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
       throw new StudentDataCollectionAPIRuntimeException("Exception occurred while writing PDF report for grade enrollment :: " + e.getMessage());
    }
  }

  protected District validateAndReturnDistrict(SdcSchoolCollectionEntity sdcSchoolCollection){
    var district = restUtils.getDistrictByDistrictID(sdcSchoolCollection.getDistrictID().toString());
    if(district.isEmpty()){
      log.info("District could not be found while writing PDF report for grade enrollment :: " + sdcSchoolCollection.getDistrictID().toString());
      throw new EntityNotFoundException(District.class, "District could not be found while writing PDF report for grade enrollment :: ", sdcSchoolCollection.getDistrictID().toString());
    }

    return district.get();
  }

  protected School validateAndReturnSchool(SdcSchoolCollectionEntity sdcSchoolCollection){
    var school = restUtils.getSchoolBySchoolID(sdcSchoolCollection.getSchoolID().toString());
    if(school.isEmpty()){
      log.info("School could not be found while writing PDF report for grade enrollment :: " + sdcSchoolCollection.getSchoolID().toString());
      throw new EntityNotFoundException(School.class, "School could not be found while writing PDF report for grade enrollment :: ", sdcSchoolCollection.getSchoolID().toString());
    }

    return school.get();
  }

  protected void setReportTombstoneValues(SdcSchoolCollectionEntity sdcSchoolCollection, BaseReportNode reportNode){
    var district = validateAndReturnDistrict(sdcSchoolCollection);
    var school = validateAndReturnSchool(sdcSchoolCollection);

    reportNode.setReportGeneratedDate("Report Generated: " + LocalDate.now().format(formatter));
    reportNode.setDistrictNumberAndName(district.getDistrictNumber() + " - " + district.getDisplayName());
    reportNode.setCollectionNameAndYear(StringUtils.capitalize(sdcSchoolCollection.getCollectionEntity().getCollectionTypeCode().toLowerCase()) + " " + sdcSchoolCollection.getCollectionEntity().getOpenDate().getYear() + " Collection");
    reportNode.setSchoolMincodeAndName(school.getMincode() + " - " + school.getDisplayName());
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
