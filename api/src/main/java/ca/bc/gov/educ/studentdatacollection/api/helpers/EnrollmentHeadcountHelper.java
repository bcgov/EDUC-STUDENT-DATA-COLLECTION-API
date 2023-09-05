package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
public class EnrollmentHeadcountHelper extends HeadcountHelper {
  private static final String TOTAL_FTE_TITLE = "FTE Total";
  private static final String ELIGIBLE_FTE_TITLE = "Eligible for FTE";
  private static final String HEADCOUNT_TITLE = "Headcount";
  private static final String SCHOOL_AGED_TITLE = "School Aged";
  private static final String ADULT_TITLE = "Adult";
  private static final String ALL_STUDENT_TITLE = "All Students";
  private static final String TOTAL_TITLE = "Total";

  public EnrollmentHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
  }


  public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);

    List<EnrollmentHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolId(previousCollectionID);
    List<HeadcountTableData> previousCollectionData = convertEnrollmentHeadcountResults(previousCollectionRawData);
    List<HeadcountHeader> previousHeadcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(previousCollectionData), getGradesHeadcountTotals(previousCollectionData));
    setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
  }

  public HeadcountHeader getGradesHeadcountTotals(List<HeadcountTableData> headcountTableDataList) {
    HeadcountHeader headcountTotals = new HeadcountHeader();
    headcountTotals.setTitle("Grade Headcount");
    headcountTotals.setOrderedColumnTitles(Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList());
    headcountTotals.setColumns(new HashMap<>());
    for (HeadCountTableDataRow row : headcountTableDataList.get(2).getRows()) {
      if(!row.getTitle().equals(TOTAL_TITLE)) {
        String title = row.getTitle();
        HeadcountHeaderColumn headcountHeaderColumn = new HeadcountHeaderColumn();
        headcountHeaderColumn.setCurrentValue(row.getColumnTitleAndValueMap().get(HEADCOUNT_TITLE));
        headcountTotals.getColumns().put(title, headcountHeaderColumn);
      }
    }
    return headcountTotals;
  }

  public HeadcountHeader getStudentsHeadcountTotals(List<HeadcountTableData> headcountTableDataList) {
    HeadcountHeader studentTotals = new HeadcountHeader();
    studentTotals.setTitle("Student Headcount");
    studentTotals.setOrderedColumnTitles(List.of(SCHOOL_AGED_TITLE, ADULT_TITLE, ALL_STUDENT_TITLE));
    studentTotals.setColumns(new HashMap<>());

    for (HeadcountTableData tableData : headcountTableDataList) {
      String title = tableData.getTitle();
      HeadcountHeaderColumn headcountHeaderColumn = new HeadcountHeaderColumn();
      headcountHeaderColumn.setCurrentValue(String.valueOf(
              tableData.getRows().stream()
                      .filter(row -> row.getTitle().equals(TOTAL_TITLE))
                      .mapToLong(row -> Long.parseLong(row.getColumnTitleAndValueMap().get(HEADCOUNT_TITLE)))
                      .sum()));
      studentTotals.getColumns().put(title, headcountHeaderColumn);
    }
    return studentTotals;
  }

  public List<HeadcountTableData> convertEnrollmentHeadcountResults(List<EnrollmentHeadcountResult> results) {
    List<HeadcountTableData> headcountTableDataList = new ArrayList<>();
    List<String> gradeCodes = Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList();
    List<String> columnTitles = Arrays.asList(HEADCOUNT_TITLE, ELIGIBLE_FTE_TITLE, TOTAL_FTE_TITLE);

    for (String title : Arrays.asList(SCHOOL_AGED_TITLE, ADULT_TITLE, ALL_STUDENT_TITLE)) {
      List<HeadCountTableDataRow> rows = new ArrayList<>();

      for (String gradeCode : gradeCodes) {
        EnrollmentHeadcountResult result = results.stream()
                .filter(value -> value.getEnrolledGradeCode().equals(gradeCode))
                .findFirst()
                .orElse(null);
        rows.add(buildDataRow(result, title, gradeCode));
      }
      String[] keys = { HEADCOUNT_TITLE, ELIGIBLE_FTE_TITLE,TOTAL_FTE_TITLE };
      rows.add(calculateSummaryRow(rows, keys, TOTAL_TITLE));
      headcountTableDataList.add(buildHeadcountTableData(title, rows, columnTitles));
    }
    return headcountTableDataList;
  }

  public HeadCountTableDataRow buildDataRow(EnrollmentHeadcountResult result, String title, String gradeCode) {
    Map<String, String> valuesMap = new HashMap<>();

    if(result != null) {
      switch (title) {
        case SCHOOL_AGED_TITLE -> {
          valuesMap.put(HEADCOUNT_TITLE, String.valueOf(result.getSchoolAgedHeadcount()));
          valuesMap.put(ELIGIBLE_FTE_TITLE, String.valueOf(result.getSchoolAgedEligibleForFte()));
          valuesMap.put(TOTAL_FTE_TITLE, String.valueOf(result.getSchoolAgedFteTotal()));
        }
        case ADULT_TITLE -> {
          valuesMap.put(HEADCOUNT_TITLE, String.valueOf(result.getAdultHeadcount()));
          valuesMap.put(ELIGIBLE_FTE_TITLE, String.valueOf(result.getAdultEligibleForFte()));
          valuesMap.put(TOTAL_FTE_TITLE, String.valueOf(result.getAdultFteTotal()));
        }
        case ALL_STUDENT_TITLE -> {
          valuesMap.put(HEADCOUNT_TITLE, String.valueOf(result.getTotalHeadcount()));
          valuesMap.put(ELIGIBLE_FTE_TITLE, String.valueOf(result.getTotalEligibleForFte()));
          valuesMap.put(TOTAL_FTE_TITLE, String.valueOf(result.getTotalFteTotal()));
        }
        default -> log.warn("Unexpected case in buildDataRow. This should not have happened.");
      }
    } else {
      valuesMap.put(HEADCOUNT_TITLE, "0");
      valuesMap.put(ELIGIBLE_FTE_TITLE, "0");
      valuesMap.put(TOTAL_FTE_TITLE, "0");
    }
    return HeadCountTableDataRow.builder()
            .title(gradeCode)
            .columnTitleAndValueMap(valuesMap)
            .build();
  }
}
