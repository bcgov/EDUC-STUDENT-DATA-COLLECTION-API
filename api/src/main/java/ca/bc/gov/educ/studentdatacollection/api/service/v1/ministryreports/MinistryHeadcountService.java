package ca.bc.gov.educ.studentdatacollection.api.service.v1.ministryreports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolAddressHeaders;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.util.LocalDateTimeUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolAddressHeaders.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.SCHOOL_NAME;


@Service
@Slf4j
@RequiredArgsConstructor
public class MinistryHeadcountService {
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final RestUtils restUtils;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private static final String COLLECTION_ID = "collectionID";

  public SimpleHeadcountResultsTable getAllSchoolEnrollmentHeadcounts(UUID collectionID) {
    List<SchoolHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getAllEnrollmentHeadcountsByCollectionId(collectionID);
    var collectionOpt = collectionRepository.findById(collectionID);
    if(collectionOpt.isEmpty()){
      throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
    }
    var collection = collectionOpt.get();

    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (SchoolEnrolmentHeader header : SchoolEnrolmentHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, String>>();
    collectionRawData.stream().forEach(schoolHeadcountResult -> {
      var school = restUtils.getAllSchoolBySchoolID(schoolHeadcountResult.getSchoolID()).get();

      var rowMap = new HashMap<String, String>();
      rowMap.put(SCHOOL_YEAR.getCode(), LocalDateTimeUtil.getSchoolYearString(collection));
      rowMap.put(DISTRICT_NUMBER.getCode(), school.getMincode().substring(0,3));
      rowMap.put(SCHOOL_NUMBER.getCode(), school.getSchoolNumber());
      rowMap.put(SCHOOL_NAME.getCode(), school.getDisplayName());
      rowMap.put(FACILITY_TYPE.getCode(), school.getFacilityTypeCode());
      rowMap.put(SCHOOL_CATEGORY.getCode(), school.getSchoolCategoryCode());
      rowMap.put(GRADE_RANGE.getCode(),  TransformUtil.getGradesOfferedString(school));
      rowMap.put(REPORT_DATE.getCode(), collection.getSnapshotDate().toString());
      rowMap.put(KIND_HT_COUNT.getCode(), schoolHeadcountResult.getKindHCount());
      rowMap.put(KIND_FT_COUNT.getCode(), schoolHeadcountResult.getKindFCount());
      rowMap.put(GRADE_01_COUNT.getCode(), schoolHeadcountResult.getGrade1Count());
      rowMap.put(GRADE_02_COUNT.getCode(), schoolHeadcountResult.getGrade2Count());
      rowMap.put(GRADE_03_COUNT.getCode(), schoolHeadcountResult.getGrade3Count());
      rowMap.put(GRADE_04_COUNT.getCode(), schoolHeadcountResult.getGrade4Count());
      rowMap.put(GRADE_05_COUNT.getCode(), schoolHeadcountResult.getGrade5Count());
      rowMap.put(GRADE_06_COUNT.getCode(), schoolHeadcountResult.getGrade6Count());
      rowMap.put(GRADE_07_COUNT.getCode(), schoolHeadcountResult.getGrade7Count());
      rowMap.put(GRADE_08_COUNT.getCode(), schoolHeadcountResult.getGrade8Count());
      rowMap.put(GRADE_09_COUNT.getCode(), schoolHeadcountResult.getGrade9Count());
      rowMap.put(GRADE_10_COUNT.getCode(), schoolHeadcountResult.getGrade10Count());
      rowMap.put(GRADE_11_COUNT.getCode(), schoolHeadcountResult.getGrade11Count());
      rowMap.put(GRADE_12_COUNT.getCode(), schoolHeadcountResult.getGrade12Count());
      rows.add(rowMap);
    });
    resultsTable.setRows(rows);
    return resultsTable;
  }

  public SimpleHeadcountResultsTable getIndySchoolsEnrollmentHeadcounts(UUID collectionID) {
    List<IndySchoolHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);
    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (IndySchoolEnrolmentHeadcountHeader header : IndySchoolEnrolmentHeadcountHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, String>>();
    collectionRawData.stream().forEach(indySchoolHeadcountResult -> {
      var school = restUtils.getAllSchoolBySchoolID(indySchoolHeadcountResult.getSchoolID()).get();

      if(SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
        var rowMap = new HashMap<String, String>();
        rowMap.put(SCHOOL.getCode(), school.getDisplayName());
        rowMap.put(KIND_HT.getCode(), indySchoolHeadcountResult.getKindHCount());
        rowMap.put(KIND_FT.getCode(), indySchoolHeadcountResult.getKindFCount());
        rowMap.put(GRADE_01.getCode(), indySchoolHeadcountResult.getGrade1Count());
        rowMap.put(GRADE_02.getCode(), indySchoolHeadcountResult.getGrade2Count());
        rowMap.put(GRADE_03.getCode(), indySchoolHeadcountResult.getGrade3Count());
        rowMap.put(GRADE_04.getCode(), indySchoolHeadcountResult.getGrade4Count());
        rowMap.put(GRADE_05.getCode(), indySchoolHeadcountResult.getGrade5Count());
        rowMap.put(GRADE_06.getCode(), indySchoolHeadcountResult.getGrade6Count());
        rowMap.put(GRADE_07.getCode(), indySchoolHeadcountResult.getGrade7Count());
        rowMap.put(GRADE_EU.getCode(), indySchoolHeadcountResult.getGradeEUCount());
        rowMap.put(GRADE_08.getCode(), indySchoolHeadcountResult.getGrade8Count());
        rowMap.put(GRADE_09.getCode(), indySchoolHeadcountResult.getGrade9Count());
        rowMap.put(GRADE_10.getCode(), indySchoolHeadcountResult.getGrade10Count());
        rowMap.put(GRADE_11.getCode(), indySchoolHeadcountResult.getGrade11Count());
        rowMap.put(GRADE_12.getCode(), indySchoolHeadcountResult.getGrade12Count());
        rowMap.put(GRADE_SU.getCode(), indySchoolHeadcountResult.getGradeSUCount());
        rowMap.put(GRADE_GA.getCode(), indySchoolHeadcountResult.getGradeGACount());
        rowMap.put(GRADE_HS.getCode(), indySchoolHeadcountResult.getGradeHSCount());
        rowMap.put(TOTAL.getCode(), TransformUtil.getTotalHeadcount(indySchoolHeadcountResult));
        rows.add(rowMap);
      }
    });

    resultsTable.setRows(rows);
    return resultsTable;
  }

  public SimpleHeadcountResultsTable getSchoolAddressReport(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if(entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
    }

    List<SdcSchoolCollectionEntity> schoolsInCollection = sdcSchoolCollectionRepository.findAllByCollectionEntityCollectionID(collectionID);
    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (SchoolAddressHeaders header : SchoolAddressHeaders.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, String>>();

    schoolsInCollection.forEach(result -> {
      var school = restUtils.getAllSchoolBySchoolID(String.valueOf(result.getSchoolID())).get();
      var schoolAddr = school.getAddresses().stream().filter(address -> address.getAddressTypeCode().equalsIgnoreCase("PHYSICAL")).findFirst();
      if(schoolAddr.isPresent()) {
        var address = schoolAddr.get();
        var rowMap = new HashMap<String, String>();
        rowMap.put(MINCODE.getCode(), school.getMincode());
        rowMap.put(SCHOOL_NAME.getCode(), school.getDisplayName());
        rowMap.put(ADDRESS_LINE1.getCode(),address.getAddressLine1());
        rowMap.put(ADDRESS_LINE2.getCode(), address.getAddressLine2());
        rowMap.put(CITY.getCode(), address.getCity());
        rowMap.put(PROVINCE.getCode(), address.getProvinceCode());
        rowMap.put(POSTAL.getCode(), address.getPostal());
        rows.add(rowMap);
      }
  });

    resultsTable.setRows(rows);
    return resultsTable;
  }

}
