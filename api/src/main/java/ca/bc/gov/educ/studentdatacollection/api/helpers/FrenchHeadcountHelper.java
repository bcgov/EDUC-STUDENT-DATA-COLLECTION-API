package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class FrenchHeadcountHelper extends HeadcountHelper {
  private static final String CORE_FRENCH_TITLE = "Core French";
  private static final String EARLY_FRENCH_TITLE = "Early French Immersion";
  private static final String LATE_FRENCH_TITLE = "Late French Immersion";
  private static final String FRANCO_TITLE = "Programme Francophone";
  private static final String TOTAL_FRENCH_TITLE = "All French Programs";
  private static final String ADULT_TITLE = "Adult";
  private static final String SCHOOL_AGED_TITLE = "School-Aged";
  private static final String TOTAL_GRADE_TITLE = "Total";
  private static final String ELIGIBLE_TITLE = "Eligible";
  private static final String REPORTED_TITLE = "Reported";
  private static final String NOT_REPORTED_TITLE = "Not Reported";
  private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);

  public FrenchHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
  }

  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
    List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
    FrenchHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getFrenchHeadersBySchoolId(sdcSchoolCollectionID);
    List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
    Arrays.asList(CORE_FRENCH_TITLE, EARLY_FRENCH_TITLE, LATE_FRENCH_TITLE, FRANCO_TITLE).forEach(headerTitle -> {
      HeadcountHeader headcountHeader = new HeadcountHeader();
      headcountHeader.setColumns(new HashMap<>());
      headcountHeader.setTitle(headerTitle);
      headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
      switch (headerTitle) {
        case CORE_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getTotalCoreFrench())).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedCoreFrench())).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedCoreFrench()))).build());
        }
        case EARLY_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalEarlyFrench()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedEarlyFrench()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedEarlyFrench()))).build());
        }
        case LATE_FRENCH_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalLateFrench()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedLateFrench()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedLateFrench()))).build());
        }
        case FRANCO_TITLE -> {
          headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getTotalFrancophone()).build());
          headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedFrancophone()).build());
          headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedFrancophone()))).build());
        }
      }
      headcountHeaderList.add(headcountHeader);
    });
    return headcountHeaderList;
  }

  public List<HeadcountTableData> convertHeadcountResults(List<FrenchHeadcountResult> results) {
    List<HeadcountTableData> headcountTableDataList = new ArrayList<>();
    List<String> gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
    String[] titles = { CORE_FRENCH_TITLE, EARLY_FRENCH_TITLE, LATE_FRENCH_TITLE, FRANCO_TITLE, TOTAL_FRENCH_TITLE };

    for (String title : titles) {
      List<HeadCountTableDataRow> rows = new ArrayList<>();
      for (String gradeCode : gradeCodes) {
        FrenchHeadcountResult result = results.stream()
                .filter(value -> value.getEnrolledGradeCode().equals(gradeCode))
                .findFirst()
                .orElse(null);
        rows.add(buildDataRow(result, title, gradeCode));
      }
      String[] keys = { title, SCHOOL_AGED_TITLE, ADULT_TITLE };
      rows.add(calculateSummaryRow(rows, keys, TOTAL_GRADE_TITLE));
      headcountTableDataList.add(buildHeadcountTableData(title, rows, List.of(keys)));
    }
    return headcountTableDataList;
  }

  public HeadCountTableDataRow buildDataRow(FrenchHeadcountResult result, String title, String gradeCode) {
    Map<String, String> valuesMap = new HashMap<>();

    if(result != null) {
      switch (title) {
        case CORE_FRENCH_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedCoreFrench()));
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultCoreFrench()));
          valuesMap.put(CORE_FRENCH_TITLE, String.valueOf(result.getTotalCoreFrench()));
        }
        case EARLY_FRENCH_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedEarlyFrench()));
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultEarlyFrench()));
          valuesMap.put(EARLY_FRENCH_TITLE, String.valueOf(result.getTotalEarlyFrench()));
        }
        case LATE_FRENCH_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedLateFrench()));
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultLateFrench()));
          valuesMap.put(LATE_FRENCH_TITLE, String.valueOf(result.getTotalLateFrench()));
        }
        case FRANCO_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedFrancophone()));
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultFrancophone()));
          valuesMap.put(FRANCO_TITLE, String.valueOf(result.getTotalFrancophone()));
        }
        case TOTAL_FRENCH_TITLE -> {
          valuesMap.put(SCHOOL_AGED_TITLE, String.valueOf(result.getSchoolAgedTotals()));
          valuesMap.put(ADULT_TITLE, String.valueOf(result.getAdultTotals()));
          valuesMap.put(TOTAL_FRENCH_TITLE, String.valueOf(result.getTotalTotals()));
        }
        default -> log.warn("Unexpected case in buildDataRow. This should not have happened.");
      }
    } else {
      valuesMap.put(title, "0");
      valuesMap.put(SCHOOL_AGED_TITLE, "0");
      valuesMap.put(ADULT_TITLE, "0");
    }
    return HeadCountTableDataRow.builder()
            .title(gradeCode)
            .columnTitleAndValueMap(valuesMap)
            .build();
  }
}
