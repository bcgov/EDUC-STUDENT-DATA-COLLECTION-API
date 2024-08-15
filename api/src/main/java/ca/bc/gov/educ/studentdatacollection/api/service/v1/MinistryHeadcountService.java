package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SchoolHeadcountResult;
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

  public HeadcountResultsTable getAllSchoolEnrollmentHeadcounts(UUID collectionID) {
    List<SchoolHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getAllEnrollmentHeadcountsByCollectionId(collectionID);
    var collectionOpt = collectionRepository.findById(collectionID);
    if(collectionOpt.isEmpty()){
      throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
    }
    var collection = collectionOpt.get();

    HeadcountResultsTable resultsTable = new HeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (SchoolEnrolmentHeader header : SchoolEnrolmentHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, HeadcountHeaderColumn>>();
    collectionRawData.stream().forEach(schoolHeadcountResult -> {
      var school = restUtils.getSchoolBySchoolID(schoolHeadcountResult.getSchoolID()).get();

      var rowMap = new HashMap<String, HeadcountHeaderColumn>();
      rowMap.put(SCHOOL_YEAR.getCode(), getHeadcountColumn(getSchoolYearString(collection)));
      rowMap.put(DISTRICT_NUMBER.getCode(), getHeadcountColumn(school.getMincode().substring(0,3)));
      rowMap.put(SCHOOL_NUMBER.getCode(), getHeadcountColumn(school.getSchoolNumber()));
      rowMap.put(SCHOOL_NAME.getCode(), getHeadcountColumn(school.getDisplayName()));
      rowMap.put(FACILITY_TYPE.getCode(), getHeadcountColumn(school.getFacilityTypeCode()));
      rowMap.put(SCHOOL_CATEGORY.getCode(), getHeadcountColumn(school.getSchoolCategoryCode()));
      rowMap.put(REPORT_DATE.getCode(), getHeadcountColumn(collection.getSnapshotDate().toString()));
      rowMap.put(KIND_HT_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getKindHCount()));
      rowMap.put(KIND_FT_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getKindFCount()));
      rowMap.put(GRADE_01_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade1Count()));
      rowMap.put(GRADE_02_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade2Count()));
      rowMap.put(GRADE_03_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade3Count()));
      rowMap.put(GRADE_04_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade4Count()));
      rowMap.put(GRADE_05_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade5Count()));
      rowMap.put(GRADE_06_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade6Count()));
      rowMap.put(GRADE_07_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade7Count()));
      rowMap.put(GRADE_08_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade8Count()));
      rowMap.put(GRADE_09_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade9Count()));
      rowMap.put(GRADE_10_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade10Count()));
      rowMap.put(GRADE_11_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade11Count()));
      rowMap.put(GRADE_12_COUNT.getCode(), getHeadcountColumn(schoolHeadcountResult.getGrade12Count()));
      rows.add(rowMap);
    });
    resultsTable.setRows(rows);
    return resultsTable;
  }

  private HeadcountHeaderColumn getHeadcountColumn(String value){
    HeadcountHeaderColumn column = new HeadcountHeaderColumn();
    column.setCurrentValue(value);
    return column;
  }

  private String getSchoolYearString(CollectionEntity collection){
    var snapshotDateString = collection.getSnapshotDate();
    if(!collection.getCollectionTypeCode().equals(CollectionTypeCodes.SEPTEMBER.getTypeCode())){
      return snapshotDateString.minusYears(1).getYear() + "/" + snapshotDateString.getYear() + "SY";
    }else{
      return snapshotDateString.getYear() + "/" + snapshotDateString.plusYears(1).getYear() + "SY";
    }
  }

}
