package ca.bc.gov.educ.studentdatacollection.api.service.v1.ministryreports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentAuthority;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HomeLanguageSpokenCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import ca.bc.gov.educ.studentdatacollection.api.util.LocalDateTimeUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.SCHOOL;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolAddressHeaders.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.SCHOOL_NAME;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SpecialEducationHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.flagCountIfNoSchoolFundingGroup;


@Service
@Slf4j
@RequiredArgsConstructor
public class MinistryHeadcountService {
  private final CollectionRepository collectionRepository;
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final RestUtils restUtils;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  private final ValidationRulesService validationService;
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
      var schoolCategory = restUtils.getSchoolCategoryCode(school.getSchoolCategoryCode());
      var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

      var rowMap = new HashMap<String, String>();
      rowMap.put(SCHOOL_YEAR.getCode(), LocalDateTimeUtil.getSchoolYearString(collection));
      rowMap.put(DISTRICT_NUMBER.getCode(), school.getMincode().substring(0,3));
      rowMap.put(SCHOOL_NUMBER.getCode(), school.getSchoolNumber());
      rowMap.put(SCHOOL_NAME.getCode(), school.getDisplayName());
      rowMap.put(FACILITY_TYPE.getCode(), facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode());
      rowMap.put(SCHOOL_CATEGORY.getCode(), schoolCategory.isPresent() ? schoolCategory.get().getLabel() : school.getSchoolCategoryCode());
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

      var schoolFundingGroupGrades = school.getSchoolFundingGroups().stream().map(IndependentSchoolFundingGroup::getSchoolGradeCode).toList();

      if(SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
        var rowMap = new HashMap<String, String>();
        rowMap.put(SCHOOL.getCode(), school.getDisplayName());
        rowMap.put(KIND_HT.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.KINDHALF.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getKindHCount()));
        rowMap.put(KIND_FT.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.KINDFULL.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getKindFCount()));
        rowMap.put(GRADE_01.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE01.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade1Count()));
        rowMap.put(GRADE_02.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE02.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade2Count()));
        rowMap.put(GRADE_03.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE03.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade3Count()));
        rowMap.put(GRADE_04.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE04.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade4Count()));
        rowMap.put(GRADE_05.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE05.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade5Count()));
        rowMap.put(GRADE_06.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE06.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade6Count()));
        rowMap.put(GRADE_07.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE07.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade7Count()));
        rowMap.put(GRADE_EU.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.ELEMUNGR.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeEUCount()));
        rowMap.put(GRADE_08.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE08.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade8Count()));
        rowMap.put(GRADE_09.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE09.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade9Count()));
        rowMap.put(GRADE_10.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE10.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade10Count()));
        rowMap.put(GRADE_11.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE11.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade11Count()));
        rowMap.put(GRADE_12.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE12.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade12Count()));
        rowMap.put(GRADE_SU.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeSUCount()));
        rowMap.put(GRADE_GA.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADUATED_ADULT.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeGACount()));
        rowMap.put(GRADE_HS.getCode(), flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.HOMESCHOOL.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeHSCount()));
        rowMap.put(IndySchoolEnrolmentHeadcountHeader.TOTAL.getCode(), TransformUtil.getTotalHeadcount(indySchoolHeadcountResult));
        rows.add(rowMap);
      }
    });

    resultsTable.setRows(rows);
    return resultsTable;
  }

  public SimpleHeadcountResultsTable getSpecialEducationHeadcountsForIndependentsByCollectionID(UUID collectionID) {
    List<IndySpecialEdAdultHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryForIndiesAndOffshoreByCollectionId(collectionID);
    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (SpecialEducationHeadcountHeader header : SpecialEducationHeadcountHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, String>>();
    collectionRawData.stream().forEach(specialEdHeadcountResult -> {
      var school = restUtils.getAllSchoolBySchoolID(specialEdHeadcountResult.getSchoolID()).get();

      if(SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
        var rowMap = new HashMap<String, String>();
        rowMap.put(SpecialEducationHeadcountHeader.SCHOOL.getCode(), school.getDisplayName());
        rowMap.put(A.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdACodes(), specialEdHeadcountResult.getAdultsInSpecialEdA()));
        rowMap.put(B.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdBCodes(), specialEdHeadcountResult.getAdultsInSpecialEdB()));
        rowMap.put(C.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdCCodes(), specialEdHeadcountResult.getAdultsInSpecialEdC()));
        rowMap.put(D.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdDCodes(), specialEdHeadcountResult.getAdultsInSpecialEdD()));
        rowMap.put(E.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdECodes(), specialEdHeadcountResult.getAdultsInSpecialEdE()));
        rowMap.put(F.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdFCodes(), specialEdHeadcountResult.getAdultsInSpecialEdF()));
        rowMap.put(G.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdGCodes(), specialEdHeadcountResult.getAdultsInSpecialEdG()));
        rowMap.put(H.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdHCodes(), specialEdHeadcountResult.getAdultsInSpecialEdH()));
        rowMap.put(K.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdKCodes(), specialEdHeadcountResult.getAdultsInSpecialEdK()));
        rowMap.put(P.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdPCodes(), specialEdHeadcountResult.getAdultsInSpecialEdP()));
        rowMap.put(Q.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdQCodes(), specialEdHeadcountResult.getAdultsInSpecialEdQ()));
        rowMap.put(R.getCode(), TransformUtil.flagSpecialEdHeadcountIfRequired(specialEdHeadcountResult.getSpecialEdRCodes(), specialEdHeadcountResult.getAdultsInSpecialEdR()));
        rowMap.put(SpecialEducationHeadcountHeader.TOTAL.getCode(), TransformUtil.getTotalHeadcount(specialEdHeadcountResult));
        rows.add(rowMap);
      }
    });

    resultsTable.setRows(rows);
    return resultsTable;
  }

  public SimpleHeadcountResultsTable getSpecialEducationFundingHeadcountsForIndependentsByCollectionID(UUID collectionID) {
    List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsByCollectionId(collectionID);
    var lastSeptCollectionOpt = sdcSchoolCollectionRepository.findLastCollectionByType(CollectionTypeCodes.SEPTEMBER.getTypeCode(), collectionID);
    if(lastSeptCollectionOpt.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
    }
    List<SpecialEdHeadcountResult> lastSeptCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsByCollectionId(lastSeptCollectionOpt.get().getCollectionID());
    var mappedSeptData = generateCollectionMap(lastSeptCollectionRawData);

    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (IndySpecialEducationFundingHeadcountHeader header : IndySpecialEducationFundingHeadcountHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);

    int totSeptLevel1s = 0;
    int totSeptLevel2s = 0;
    int totSeptLevel3s = 0;
    int totFebLevel1s = 0;
    int totFebLevel2s = 0;
    int totFebLevel3s = 0;
    int totPositiveChangeLevel1s = 0;
    int totPositiveChangeLevel2s = 0;
    int totPositiveChangeLevel3s = 0;
    int totNetLevel1s = 0;
    int totNetLevel2s = 0;
    int totNetLevel3s = 0;

    var rows = new ArrayList<Map<String, String>>();
    for(SpecialEdHeadcountResult februaryCollectionRecord: collectionRawData) {
      var septCollectionRecord = mappedSeptData.get(februaryCollectionRecord.getSchoolID());

      var school = restUtils.getAllSchoolBySchoolID(februaryCollectionRecord.getSchoolID()).get();
      var district = restUtils.getDistrictByDistrictID(school.getDistrictId()).get();
      IndependentAuthority authority = null;
      if(school.getIndependentAuthorityId() != null) {
        authority = restUtils.getAuthorityByAuthorityID(school.getIndependentAuthorityId()).get();
      }

      if(SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
        var positiveChangeLevel1 = getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : null, februaryCollectionRecord.getLevelOnes());
        var positiveChangeLevel2 = getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : null, februaryCollectionRecord.getLevelTwos());
        var positiveChangeLevel3 = getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : null, februaryCollectionRecord.getLevelThrees());
        var netChangeLevel1 = getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : null, februaryCollectionRecord.getLevelOnes());
        var netChangeLevel2 = getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : null, februaryCollectionRecord.getLevelTwos());
        var netChangeLevel3 = getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : null, februaryCollectionRecord.getLevelThrees());

        var rowMap = new HashMap<String, String>();
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.DISTRICT_NUMBER.getCode(), district.getDistrictNumber());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.DISTRICT_NAME.getCode(), district.getDisplayName());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NUMBER.getCode(), authority != null ? authority.getAuthorityNumber() : null );
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NAME.getCode(), authority != null ? authority.getDisplayName() : null );
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.MINCODE.getCode(), school.getMincode());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.SCHOOL_NAME.getCode(), school.getDisplayName());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_1.getCode(), septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : null);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_2.getCode(), septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : null);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_3.getCode(), septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : null);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_1.getCode(), februaryCollectionRecord.getLevelOnes());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_2.getCode(), februaryCollectionRecord.getLevelTwos());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_3.getCode(), februaryCollectionRecord.getLevelThrees());
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_1.getCode(), positiveChangeLevel1);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_2.getCode(), positiveChangeLevel2);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_3.getCode(), positiveChangeLevel3);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_1.getCode(), netChangeLevel1);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_2.getCode(), netChangeLevel2);
        rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_3.getCode(), netChangeLevel3);
        rows.add(rowMap);

        if(septCollectionRecord != null){
          totSeptLevel1s = addValueIfExists(totSeptLevel1s, septCollectionRecord.getLevelOnes());
          totSeptLevel2s = addValueIfExists(totSeptLevel2s, septCollectionRecord.getLevelTwos());
          totSeptLevel3s = addValueIfExists(totSeptLevel3s, septCollectionRecord.getLevelThrees());
        }
        totFebLevel1s = addValueIfExists(totFebLevel1s, februaryCollectionRecord.getLevelOnes());
        totFebLevel2s = addValueIfExists(totFebLevel2s, februaryCollectionRecord.getLevelTwos());
        totFebLevel3s = addValueIfExists(totFebLevel3s, februaryCollectionRecord.getLevelThrees());
        totPositiveChangeLevel1s = addValueIfExists(totPositiveChangeLevel1s, positiveChangeLevel1);
        totPositiveChangeLevel2s = addValueIfExists(totPositiveChangeLevel2s, positiveChangeLevel2);
        totPositiveChangeLevel3s = addValueIfExists(totPositiveChangeLevel3s, positiveChangeLevel3);
        totNetLevel1s = addValueIfExists(totNetLevel1s, netChangeLevel1);
        totNetLevel2s = addValueIfExists(totNetLevel2s, netChangeLevel2);
        totNetLevel3s = addValueIfExists(totNetLevel3s, netChangeLevel3);
      }
    }

    rows.add(getIndependentSchoolFundingTotalRow(totSeptLevel1s, totSeptLevel2s, totSeptLevel3s, totFebLevel1s, totFebLevel2s,
            totFebLevel3s, totPositiveChangeLevel1s, totPositiveChangeLevel2s, totPositiveChangeLevel3s, totNetLevel1s, totNetLevel2s, totNetLevel3s));

    resultsTable.setRows(rows);
    return resultsTable;
  }

  private HashMap<String, String> getIndependentSchoolFundingTotalRow(int totSeptLevel1s,
                                                                      int totSeptLevel2s,
                                                                      int totSeptLevel3s,
                                                                      int totFebLevel1s,
                                                                      int totFebLevel2s,
                                                                      int totFebLevel3s,
                                                                      int totPositiveChangeLevel1s,
                                                                      int totPositiveChangeLevel2s,
                                                                      int totPositiveChangeLevel3s,
                                                                      int totNetLevel1s,
                                                                      int totNetLevel2s,
                                                                      int totNetLevel3s){
    var rowMap = new HashMap<String, String>();
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.DISTRICT_NUMBER.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.DISTRICT_NAME.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NUMBER.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NAME.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.MINCODE.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.SCHOOL_NAME.getCode(), null);
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_1.getCode(), Integer.toString(totSeptLevel1s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_2.getCode(), Integer.toString(totSeptLevel2s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.SEPT_LEVEL_3.getCode(), Integer.toString(totSeptLevel3s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_1.getCode(), Integer.toString(totFebLevel1s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_2.getCode(), Integer.toString(totFebLevel2s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.FEB_LEVEL_3.getCode(), Integer.toString(totFebLevel3s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_1.getCode(), Integer.toString(totPositiveChangeLevel1s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_2.getCode(), Integer.toString(totPositiveChangeLevel2s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.POSITIVE_CHANGE_LEVEL_3.getCode(), Integer.toString(totPositiveChangeLevel3s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_1.getCode(), Integer.toString(totNetLevel1s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_2.getCode(), Integer.toString(totNetLevel2s));
    rowMap.put(IndySpecialEducationFundingHeadcountHeader.NET_CHANGE_LEVEL_3.getCode(), Integer.toString(totNetLevel3s));
    return rowMap;
  }

  private int addValueIfExists(int totalValue, String actualValue){
    if(StringUtils.isNumeric(actualValue)){
      totalValue = totalValue + Integer.parseInt(actualValue);
    }
    return totalValue;
  }

  private String getNetChange(String septValue, String febValue){
    if(StringUtils.isNumeric(septValue) && StringUtils.isNumeric(febValue)){
      var change = Integer.parseInt(febValue) - Integer.parseInt(septValue);
      return Integer.toString(change);
    }
    return null;
  }

  private String getPositiveChange(String septValue, String febValue){
    if(StringUtils.isNumeric(septValue) && StringUtils.isNumeric(febValue)){
      var change = Integer.parseInt(febValue) - Integer.parseInt(septValue);
      if(change > 0){
        return Integer.toString(change);
      }
    }
    return null;
  }

  private Map<String, SpecialEdHeadcountResult> generateCollectionMap(List<SpecialEdHeadcountResult> collectionRawData){
    return collectionRawData.stream().collect(Collectors.toMap(SpecialEdHeadcountResult::getSchoolID, item -> item));
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

  public SimpleHeadcountResultsTable getOffshoreSchoolEnrollmentHeadcounts(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if(entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
    }

    List<IndySchoolHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);
    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    var headerList = new ArrayList<String>();
    for (IndySchoolEnrolmentHeadcountHeader header : IndySchoolEnrolmentHeadcountHeader.values()) {
      headerList.add(header.getCode());
    }
    resultsTable.setHeaders(headerList);
    var rows = new ArrayList<Map<String, String>>();

    collectionRawData.forEach(indySchoolHeadcountResult -> {
      var school = restUtils.getAllSchoolBySchoolID(indySchoolHeadcountResult.getSchoolID()).get();

      if(school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode())) {
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
        rowMap.put(IndySchoolEnrolmentHeadcountHeader.TOTAL.getCode(), TransformUtil.getTotalHeadcount(indySchoolHeadcountResult));
        rows.add(rowMap);
      }
    });

    resultsTable.setRows(rows);
    return resultsTable;
  }

  public SimpleHeadcountResultsTable getOffshoreSpokenLanguageHeadcounts(UUID collectionID) {
    Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
    if(entityOptional.isEmpty()) {
      throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
    }
    List<SpokenLanguageHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllHomeLanguageSpokenCodesForIndiesAndOffshoreInCollection(collectionID);
    var headerList = new ArrayList<String>();
    List<String> columns = validationService.getActiveHomeLanguageSpokenCodes().stream().filter(languages ->
                    results.stream().anyMatch(language -> language.getSpokenLanguageCode().equalsIgnoreCase(languages.getHomeLanguageSpokenCode())))
            .map(HomeLanguageSpokenCode::getDescription).toList();

    SimpleHeadcountResultsTable resultsTable = new SimpleHeadcountResultsTable();
    headerList.add(SCHOOL.getCode());
    headerList.addAll(columns);
    resultsTable.setHeaders(headerList);

    var rows = new ArrayList<Map<String, String>>();

    results.forEach(languageResult -> {
      var school = restUtils.getSchoolBySchoolID(languageResult.getSchoolID()).get();
      if(school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode())) {
        var rowMap = new HashMap<String, String>();

        var existingRowOpt = rows.stream().filter(row -> row.containsValue(school.getDisplayName())).findFirst();
        if(existingRowOpt.isPresent()) {
          //if school row already exist
          var existingRow = existingRowOpt.get();
          var spokenDesc = validationService.getActiveHomeLanguageSpokenCodes().stream()
                  .filter(code -> code.getHomeLanguageSpokenCode().equalsIgnoreCase(languageResult.getSpokenLanguageCode())).findFirst();
          existingRow.put(spokenDesc.get().getDescription(), languageResult.getHeadcount());

        } else {
          //create new rows
          rowMap.put(SCHOOL.getCode(), school.getDisplayName());
          //look-up spoken language code and add its value
          var spokenDesc = validationService.getActiveHomeLanguageSpokenCodes().stream()
                  .filter(code -> code.getHomeLanguageSpokenCode().equalsIgnoreCase(languageResult.getSpokenLanguageCode())).findFirst();
          columns.forEach(column -> {
            if(spokenDesc.get().getDescription().equalsIgnoreCase(column)) {
              rowMap.put(spokenDesc.get().getDescription(), languageResult.getHeadcount());
            } else {
              rowMap.put(column, "0");
            }
          });
          rows.add(rowMap);
        }
      }
    });

    resultsTable.setRows(rows);
    return resultsTable;
  }

}
