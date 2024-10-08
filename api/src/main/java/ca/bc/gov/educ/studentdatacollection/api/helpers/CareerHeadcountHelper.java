package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class CareerHeadcountHelper extends HeadcountHelper<CareerHeadcountResult> {
  private static final String CAREER_PREPARATION_TITLE = "Career Preparation";
  private static final String COOP_EDUCATION_TITLE = "Co-Operative Education";
  private static final String YOUTH_WORK_IN_TRADES_TITLE = "Youth Work in Trades Program";
  private static final String TECH_YOUTH_TITLE = "Career Technical or Youth Train in Trades";
  private static final String ALL_CAREER_PROGRAM_TITLE = "All Career Programs";
  private static final String XA_CODE_TITLE = "XA - Business & Applied Business";
  private static final String XB_CODE_TITLE = "XB - Fine Arts, Design, & Media";
  private static final String XC_CODE_TITLE = "XC - Fitness & Recreation";
  private static final String XD_CODE_TITLE = "XD - Health & Human Services";
  private static final String XE_CODE_TITLE = "XE - Liberal Arts & Humanities";
  private static final String XF_CODE_TITLE = "XF - Science & Applied Science";
  private static final String XG_CODE_TITLE = "XG - Tourism, Hospitality & Foods";
  private static final String XH_CODE_TITLE = "XH - Trades & Technology";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
  private static final String PREP_TOTAL_KEY = "preparationTotal";
  private static final String PREP_XA_KEY = "preparationXA";
  private static final String PREP_XB_KEY = "preparationXB";
  private static final String PREP_XC_KEY = "preparationXC";
  private static final String PREP_XD_KEY = "preparationXD";
  private static final String PREP_XE_KEY = "preparationXE";
  private static final String PREP_XF_KEY = "preparationXF";
  private static final String PREP_XG_KEY = "preparationXG";
  private static final String PREP_XH_KEY = "preparationXH";
  private static final String COOP_TOTAL_KEY = "coopTotal";
  private static final String COOP_XA_KEY = "coopXA";
  private static final String COOP_XB_KEY = "coopXB";
  private static final String COOP_XC_KEY = "coopXC";
  private static final String COOP_XD_KEY = "coopXD";
  private static final String COOP_XE_KEY = "coopXE";
  private static final String COOP_XF_KEY = "coopXF";
  private static final String COOP_XG_KEY = "coopXG";
  private static final String COOP_XH_KEY = "coopXH";
  private static final String TECH_YOUTH_TOTAL_KEY = "TechYouthTotal";
  private static final String TECH_YOUTH_XA_KEY = "TechYouthXA";
  private static final String TECH_YOUTH_XB_KEY = "TechYouthXB";
  private static final String TECH_YOUTH_XC_KEY = "TechYouthXC";
  private static final String TECH_YOUTH_XD_KEY = "TechYouthXD";
  private static final String TECH_YOUTH_XE_KEY = "TechYouthXE";
  private static final String TECH_YOUTH_XF_KEY = "TechYouthXF";
  private static final String TECH_YOUTH_XG_KEY = "TechYouthXG";
  private static final String TECH_YOUTH_XH_KEY = "TechYouthXH";
  private static final String APPRENTICE_TOTAL_KEY = "ApprenticeTotal";
  private static final String APPRENTICE_XA_KEY = "ApprenticeXA";
  private static final String APPRENTICE_XB_KEY = "ApprenticeXB";
  private static final String APPRENTICE_XC_KEY = "ApprenticeXC";
  private static final String APPRENTICE_XD_KEY = "ApprenticeXD";
  private static final String APPRENTICE_XE_KEY = "ApprenticeXE";
  private static final String APPRENTICE_XF_KEY = "ApprenticeXF";
  private static final String APPRENTICE_XG_KEY = "ApprenticeXG";
  private static final String APPRENTICE_XH_KEY = "ApprenticeXH";
  private static final String ALL_TOTAL_KEY = "AllTotal";
  private static final String ALL_XA_KEY = "AllXA";
  private static final String ALL_XB_KEY = "AllXB";
  private static final String ALL_XC_KEY = "AllXC";
  private static final String ALL_XD_KEY = "AllXD";
  private static final String ALL_XE_KEY = "AllXE";
  private static final String ALL_XF_KEY = "AllXF";
  private static final String ALL_XG_KEY = "AllXG";
  private static final String ALL_XH_KEY = "AllXH";
  public static final String TITLE="title";
  private final RestUtils restUtils;
  protected Map<String, String> perSchoolRowTitles;
  public static final String SCHOOL_NAME_KEY="schoolName";
  public static final String SECTION="section";
  private static final String TOTAL_TITLE = "Total";

  public CareerHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository, RestUtils restUtils) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository, restUtils);
    this.restUtils = restUtils;
    headcountMethods = getHeadcountMethods();
    sectionTitles = getSelectionTitles();
    rowTitles = getRowTitles();
    gradeCodes = SchoolGradeCodes.get8PlusGrades();
    perSchoolRowTitles = getPerSchoolReportRowTitles();
  }
  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, false);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setComparisonValuesForDistrictReporting(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
    List<CareerHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcDistrictCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setComparisonValuesForDistrictBySchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, List<HeadcountHeader> headcountHeaderList, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousCollectionIDByDistrictCollectionID(sdcDistrictCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID, true);
    List<CareerHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySchoolIdAndBySdcDistrictCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertCareerBySchoolHeadcountResults(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawData);
    setResultsTableComparisonValuesDynamicNested(collectionData, previousCollectionData);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public void setResultsTableComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, HeadcountResultsTable collectionData) {
    UUID previousCollectionID = getPreviousCollectionID(sdcSchoolCollectionEntity);
    List<CareerHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(previousCollectionID);
    HeadcountResultsTable previousCollectionData = convertHeadcountResults(collectionRawData);
    setResultsTableComparisonValues(collectionData, previousCollectionData);
  }
  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID, boolean isDistrict) {
    CareerHeadcountHeaderResult result = isDistrict
            ? sdcSchoolCollectionStudentRepository.getCareerHeadersBySdcDistrictCollectionId(sdcSchoolCollectionID)
            : sdcSchoolCollectionStudentRepository.getCareerHeadersBySchoolId(sdcSchoolCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    Arrays.asList(CAREER_PREPARATION_TITLE, COOP_EDUCATION_TITLE, YOUTH_WORK_IN_TRADES_TITLE, TECH_YOUTH_TITLE).forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);
      headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
      switch (headerTitle) {
        case CAREER_PREPARATION_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getEligCareerPrep())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedCareerPrep())).build());
        }
        case COOP_EDUCATION_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligCoopEduc()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedCoopEduc()).build());
        }
        case YOUTH_WORK_IN_TRADES_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligApprentice()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedApprentice()).build());
        }
        case TECH_YOUTH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligTechOrYouth()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedTechOrYouth()).build());
        }
        default -> {
          log.error("Unexpected header title.  This cannot happen::" + headerTitle);
          throw new StudentDataCollectionAPIRuntimeException("Unexpected header title.  This cannot happen::" + headerTitle);
        }
      }
      headcountHeaderList.add(headcountHeader);
    });
    return headcountHeaderList;
  }
  private Map<String, Function<CareerHeadcountResult, String>> getHeadcountMethods() {
    Map<String, Function<CareerHeadcountResult, String>> headcountMethods = new HashMap<>();

    headcountMethods.put(PREP_TOTAL_KEY, CareerHeadcountResult::getPreparationTotal);
    headcountMethods.put(PREP_XA_KEY, CareerHeadcountResult::getPreparationXA);
    headcountMethods.put(PREP_XB_KEY, CareerHeadcountResult::getPreparationXB);
    headcountMethods.put(PREP_XC_KEY, CareerHeadcountResult::getPreparationXC);
    headcountMethods.put(PREP_XD_KEY, CareerHeadcountResult::getPreparationXD);
    headcountMethods.put(PREP_XE_KEY, CareerHeadcountResult::getPreparationXE);
    headcountMethods.put(PREP_XF_KEY, CareerHeadcountResult::getPreparationXF);
    headcountMethods.put(PREP_XG_KEY, CareerHeadcountResult::getPreparationXG);
    headcountMethods.put(PREP_XH_KEY, CareerHeadcountResult::getPreparationXH);
    headcountMethods.put(COOP_TOTAL_KEY, CareerHeadcountResult::getCoopTotal);
    headcountMethods.put(COOP_XA_KEY, CareerHeadcountResult::getCoopXA);
    headcountMethods.put(COOP_XB_KEY, CareerHeadcountResult::getCoopXB);
    headcountMethods.put(COOP_XC_KEY, CareerHeadcountResult::getCoopXC);
    headcountMethods.put(COOP_XD_KEY, CareerHeadcountResult::getCoopXD);
    headcountMethods.put(COOP_XE_KEY, CareerHeadcountResult::getCoopXE);
    headcountMethods.put(COOP_XF_KEY, CareerHeadcountResult::getCoopXF);
    headcountMethods.put(COOP_XG_KEY, CareerHeadcountResult::getCoopXG);
    headcountMethods.put(COOP_XH_KEY, CareerHeadcountResult::getCoopXH);
    headcountMethods.put(TECH_YOUTH_TOTAL_KEY, CareerHeadcountResult::getTechYouthTotal);
    headcountMethods.put(TECH_YOUTH_XA_KEY, CareerHeadcountResult::getTechYouthXA);
    headcountMethods.put(TECH_YOUTH_XB_KEY, CareerHeadcountResult::getTechYouthXB);
    headcountMethods.put(TECH_YOUTH_XC_KEY, CareerHeadcountResult::getTechYouthXC);
    headcountMethods.put(TECH_YOUTH_XD_KEY, CareerHeadcountResult::getTechYouthXD);
    headcountMethods.put(TECH_YOUTH_XE_KEY, CareerHeadcountResult::getTechYouthXE);
    headcountMethods.put(TECH_YOUTH_XF_KEY, CareerHeadcountResult::getTechYouthXF);
    headcountMethods.put(TECH_YOUTH_XG_KEY, CareerHeadcountResult::getTechYouthXG);
    headcountMethods.put(TECH_YOUTH_XH_KEY, CareerHeadcountResult::getTechYouthXH);
    headcountMethods.put(APPRENTICE_TOTAL_KEY, CareerHeadcountResult::getApprenticeTotal);
    headcountMethods.put(APPRENTICE_XA_KEY, CareerHeadcountResult::getApprenticeXA);
    headcountMethods.put(APPRENTICE_XB_KEY, CareerHeadcountResult::getApprenticeXB);
    headcountMethods.put(APPRENTICE_XC_KEY, CareerHeadcountResult::getApprenticeXC);
    headcountMethods.put(APPRENTICE_XD_KEY, CareerHeadcountResult::getApprenticeXD);
    headcountMethods.put(APPRENTICE_XE_KEY, CareerHeadcountResult::getApprenticeXE);
    headcountMethods.put(APPRENTICE_XF_KEY, CareerHeadcountResult::getApprenticeXF);
    headcountMethods.put(APPRENTICE_XG_KEY, CareerHeadcountResult::getApprenticeXG);
    headcountMethods.put(APPRENTICE_XH_KEY, CareerHeadcountResult::getApprenticeXH);
    headcountMethods.put(ALL_TOTAL_KEY, CareerHeadcountResult::getAllTotal);
    headcountMethods.put(ALL_XA_KEY, CareerHeadcountResult::getAllXA);
    headcountMethods.put(ALL_XB_KEY, CareerHeadcountResult::getAllXB);
    headcountMethods.put(ALL_XC_KEY, CareerHeadcountResult::getAllXC);
    headcountMethods.put(ALL_XD_KEY, CareerHeadcountResult::getAllXD);
    headcountMethods.put(ALL_XE_KEY, CareerHeadcountResult::getAllXE);
    headcountMethods.put(ALL_XF_KEY, CareerHeadcountResult::getAllXF);
    headcountMethods.put(ALL_XG_KEY, CareerHeadcountResult::getAllXG);
    headcountMethods.put(ALL_XH_KEY, CareerHeadcountResult::getAllXH);
    return headcountMethods;
  }
  private Map<String, String> getSelectionTitles() {
    Map<String, String> sectionTitles = new HashMap<>();

    sectionTitles.put(PREP_TOTAL_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XA_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XB_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XC_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XD_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XE_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XF_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XG_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(PREP_XH_KEY, CAREER_PREPARATION_TITLE);
    sectionTitles.put(COOP_TOTAL_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XA_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XB_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XC_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XD_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XE_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XF_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XG_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(COOP_XH_KEY, COOP_EDUCATION_TITLE);
    sectionTitles.put(TECH_YOUTH_TOTAL_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XA_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XB_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XC_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XD_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XE_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XF_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XG_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(TECH_YOUTH_XH_KEY, TECH_YOUTH_TITLE);
    sectionTitles.put(APPRENTICE_TOTAL_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XA_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XB_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XC_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XD_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XE_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XF_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XG_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(APPRENTICE_XH_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    sectionTitles.put(ALL_TOTAL_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XA_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XB_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XC_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XD_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XE_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XF_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XG_KEY, ALL_CAREER_PROGRAM_TITLE);
    sectionTitles.put(ALL_XH_KEY, ALL_CAREER_PROGRAM_TITLE);
    return sectionTitles;
  }
  private Map<String, String> getRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();

    rowTitles.put(PREP_TOTAL_KEY, CAREER_PREPARATION_TITLE);
    rowTitles.put(PREP_XA_KEY, XA_CODE_TITLE);
    rowTitles.put(PREP_XB_KEY, XB_CODE_TITLE);
    rowTitles.put(PREP_XC_KEY, XC_CODE_TITLE);
    rowTitles.put(PREP_XD_KEY, XD_CODE_TITLE);
    rowTitles.put(PREP_XE_KEY, XE_CODE_TITLE);
    rowTitles.put(PREP_XF_KEY, XF_CODE_TITLE);
    rowTitles.put(PREP_XG_KEY, XG_CODE_TITLE);
    rowTitles.put(PREP_XH_KEY, XH_CODE_TITLE);
    rowTitles.put(COOP_TOTAL_KEY, COOP_EDUCATION_TITLE);
    rowTitles.put(COOP_XA_KEY, XA_CODE_TITLE);
    rowTitles.put(COOP_XB_KEY, XB_CODE_TITLE);
    rowTitles.put(COOP_XC_KEY, XC_CODE_TITLE);
    rowTitles.put(COOP_XD_KEY, XD_CODE_TITLE);
    rowTitles.put(COOP_XE_KEY, XE_CODE_TITLE);
    rowTitles.put(COOP_XF_KEY, XF_CODE_TITLE);
    rowTitles.put(COOP_XG_KEY, XG_CODE_TITLE);
    rowTitles.put(COOP_XH_KEY, XH_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_TOTAL_KEY, TECH_YOUTH_TITLE);
    rowTitles.put(TECH_YOUTH_XA_KEY, XA_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XB_KEY, XB_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XC_KEY, XC_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XD_KEY, XD_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XE_KEY, XE_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XF_KEY, XF_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XG_KEY, XG_CODE_TITLE);
    rowTitles.put(TECH_YOUTH_XH_KEY, XH_CODE_TITLE);
    rowTitles.put(APPRENTICE_TOTAL_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    rowTitles.put(APPRENTICE_XA_KEY, XA_CODE_TITLE);
    rowTitles.put(APPRENTICE_XB_KEY, XB_CODE_TITLE);
    rowTitles.put(APPRENTICE_XC_KEY, XC_CODE_TITLE);
    rowTitles.put(APPRENTICE_XD_KEY, XD_CODE_TITLE);
    rowTitles.put(APPRENTICE_XE_KEY, XE_CODE_TITLE);
    rowTitles.put(APPRENTICE_XF_KEY, XF_CODE_TITLE);
    rowTitles.put(APPRENTICE_XG_KEY, XG_CODE_TITLE);
    rowTitles.put(APPRENTICE_XH_KEY, XH_CODE_TITLE);
    rowTitles.put(ALL_TOTAL_KEY, ALL_CAREER_PROGRAM_TITLE);
    rowTitles.put(ALL_XA_KEY, XA_CODE_TITLE);
    rowTitles.put(ALL_XB_KEY, XB_CODE_TITLE);
    rowTitles.put(ALL_XC_KEY, XC_CODE_TITLE);
    rowTitles.put(ALL_XD_KEY, XD_CODE_TITLE);
    rowTitles.put(ALL_XE_KEY, XE_CODE_TITLE);
    rowTitles.put(ALL_XF_KEY, XF_CODE_TITLE);
    rowTitles.put(ALL_XG_KEY, XG_CODE_TITLE);
    rowTitles.put(ALL_XH_KEY, XH_CODE_TITLE);
    return rowTitles;
  }

  private Map<String, String> getPerSchoolReportRowTitles() {
    Map<String, String> rowTitles = new LinkedHashMap<>();
    rowTitles.put(SCHOOL_NAME_KEY, null);
    rowTitles.put(PREP_TOTAL_KEY, CAREER_PREPARATION_TITLE);
    rowTitles.put(COOP_TOTAL_KEY, COOP_EDUCATION_TITLE);
    rowTitles.put(TECH_YOUTH_TOTAL_KEY, TECH_YOUTH_TITLE);
    rowTitles.put(APPRENTICE_TOTAL_KEY, YOUTH_WORK_IN_TRADES_TITLE);
    rowTitles.put(ALL_TOTAL_KEY, ALL_CAREER_PROGRAM_TITLE);
    return rowTitles;
  }

  public HeadcountResultsTable convertCareerBySchoolHeadcountResults(UUID sdcDistrictCollectionID, List<CareerHeadcountResult> results) {
    HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
    List<String> columnTitles = new ArrayList<>(gradeCodes);
    columnTitles.add(0, TITLE);
    columnTitles.add(TOTAL_TITLE);
    headcountResultsTable.setHeaders(columnTitles);
    headcountResultsTable.setRows(new ArrayList<>());

    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();

    List<SchoolTombstone> allSchoolsTobmstones = getAllSchoolTombstones(sdcDistrictCollectionID);

    List<SchoolTombstone> schoolResultsTombstones = results.stream()
            .map(value ->  restUtils.getSchoolBySchoolID(value.getSchoolID()).orElseThrow(() ->
                    new EntityNotFoundException(SdcSchoolCollectionStudent.class, "SchoolID", value.toString())
            )).toList();

    Set<SchoolTombstone> uniqueSchoolTombstones = new HashSet<>(schoolResultsTombstones);
    uniqueSchoolTombstones.addAll(allSchoolsTobmstones);

    uniqueSchoolTombstones.stream().distinct().forEach(school -> createSectionsBySchool(rows, results, school));
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }

  public void createSectionsBySchool(List<Map<String, HeadcountHeaderColumn>> rows, List<CareerHeadcountResult> results, SchoolTombstone schoolTombstone) {
    for (Map.Entry<String, String> title : perSchoolRowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();

      if (title.getKey().equals(SCHOOL_NAME_KEY)) {
        rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName()).build());
      } else {
        rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
      }

      BigDecimal total = BigDecimal.ZERO;
      Function<CareerHeadcountResult, String> headcountFunction = headcountMethods.get(title.getKey());
      if (headcountFunction != null) {
        for (String gradeCode : gradeCodes) {
          var result = results.stream()
                  .filter(value -> value.getEnrolledGradeCode().equals(gradeCode) && value.getSchoolID().equals(schoolTombstone.getSchoolId()))
                  .findFirst()
                  .orElse(null);
          String headcount = "0";
          if (result != null && result.getEnrolledGradeCode().equals(gradeCode)) {
            headcount = headcountFunction.apply(result);
          }
          rowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(headcount).build());
          total = total.add(new BigDecimal(headcount));
        }
        rowData.put(TOTAL_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(total)).build());
      }
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(schoolTombstone.getMincode() + " - " + schoolTombstone.getDisplayName()).build());
      rows.add(rowData);
    }
  }
}
