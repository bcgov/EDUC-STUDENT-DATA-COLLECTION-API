package ca.bc.gov.educ.studentdatacollection.api.service.v1.ministryreports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SimpleHeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.util.LocalDateTimeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;


@Service
@Slf4j
@RequiredArgsConstructor
public class MinistryHeadcountService {
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final RestUtils restUtils;
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
      var school = restUtils.getSchoolBySchoolID(schoolHeadcountResult.getSchoolID()).get();

      var rowMap = new HashMap<String, String>();
      rowMap.put(SCHOOL_YEAR.getCode(), LocalDateTimeUtil.getSchoolYearString(collection));
      rowMap.put(DISTRICT_NUMBER.getCode(), school.getMincode().substring(0,3));
      rowMap.put(SCHOOL_NUMBER.getCode(), school.getSchoolNumber());
      rowMap.put(SCHOOL_NAME.getCode(), school.getDisplayName());
      rowMap.put(FACILITY_TYPE.getCode(), school.getFacilityTypeCode());
      rowMap.put(SCHOOL_CATEGORY.getCode(), school.getSchoolCategoryCode());
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

}
