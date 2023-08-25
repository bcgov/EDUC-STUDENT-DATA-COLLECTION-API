package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.stream.IntStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentHeadcountService {
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

  private static final String TOTAL_FTE_TITLE = "FTE Total";
  private static final String ELIGIBLE_FTE_TITLE = "Eligible for FTE";
  private static final String HEADCOUNT_TITLE = "Headcount";
  private static final String SCHOOL_AGED_TITLE = "School Aged";
  private static final String ADULT_TITLE = "Adult";
  private static final String TOTAL_TITLE = "Total";

  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(UUID sdcSchoolCollectionID, boolean compare) {
    var sdcSchoolCollectionEntity = sdcSchoolCollectionRepository.findBySdcSchoolCollectionID(sdcSchoolCollectionID).orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionID", sdcSchoolCollectionID.toString()));

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSectionHeadcountsBySchoolId(sdcSchoolCollectionID);
    List<HeadcountTableData> collectionData = convertEnrollmentHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(collectionData), getGradesHeadcountTotals(collectionData));
    if(compare) {
      setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountTableDataList(collectionData).build();
  }

  private void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
    UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);

    List<EnrollmentHeadcountResult> previousCollectionRawData = sdcSchoolCollectionStudentRepository.getSectionHeadcountsBySchoolId(previousCollectionID);
    List<HeadcountTableData> previousCollectionData = convertEnrollmentHeadcountResults(previousCollectionRawData);
    List<HeadcountHeader> previousHeadcountHeaderList = Arrays.asList(getStudentsHeadcountTotals(previousCollectionData), getGradesHeadcountTotals(previousCollectionData));

    IntStream.range(0, headcountHeaderList.size())
            .forEach(i -> {
              HeadcountHeader currentHeader = headcountHeaderList.get(i);
              HeadcountHeader previousHeader = previousHeadcountHeaderList.get(i);

              currentHeader.getColumns().forEach((columnName, currentColumn) -> {
                HeadcountHeaderColumn previousColumn = previousHeader.getColumns().get(columnName);
                currentColumn.setComparisonValue(previousColumn.getCurrentValue());
              });
            });
  }

  private UUID getPreviousSeptemberCollectionID(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    int previousYear = sdcSchoolCollectionEntity.getCreateDate().minusYears(1).getYear();
    LocalDateTime startOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 1).atTime(LocalTime.MIN);
    LocalDateTime endOfCollectionDate = LocalDate.of(previousYear, Month.SEPTEMBER, 30).atTime(LocalTime.MAX);

    var septemberCollection = sdcSchoolCollectionRepository.findAllBySchoolIDAndCreateDateBetween(sdcSchoolCollectionEntity.getSchoolID(), startOfCollectionDate, endOfCollectionDate);
    if(!septemberCollection.isEmpty()) {
      return septemberCollection.get(0).getSdcSchoolCollectionID();
    } else {
      return null;
    }
  }

  private HeadcountHeader getGradesHeadcountTotals(List<HeadcountTableData> headcountTableDataList) {
    HeadcountHeader headcountTotals = new HeadcountHeader();
    headcountTotals.setTitle("Grade Headcount");
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

  private HeadcountHeader getStudentsHeadcountTotals(List<HeadcountTableData> headcountTableDataList) {
    HeadcountHeader studentTotals = new HeadcountHeader();
    studentTotals.setTitle("Student Headcount");
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
    List<HeadCountTableDataRow> schoolAgedRows = new ArrayList<>();
    List<HeadCountTableDataRow> adultRows = new ArrayList<>();
    List<HeadCountTableDataRow> totalRows = new ArrayList<>();

    for (String gradeCode : Arrays.stream(SchoolGradeCodes.values()).map(SchoolGradeCodes::getCode).toList()) {
      EnrollmentHeadcountResult result = results.stream()
              .filter(value -> value.getEnrolledGradeCode().equals(gradeCode))
              .findFirst()
              .orElse(null);
      schoolAgedRows.add(buildDataRow(result, SCHOOL_AGED_TITLE, gradeCode));
      adultRows.add(buildDataRow(result, ADULT_TITLE, gradeCode));
      totalRows.add(buildDataRow(result, TOTAL_TITLE, gradeCode));
    }
    schoolAgedRows.add(calculateSummaryRow(schoolAgedRows));
    adultRows.add(calculateSummaryRow(adultRows));
    totalRows.add(calculateSummaryRow(totalRows));

    headcountTableDataList.add(buildHeadcountTableData(SCHOOL_AGED_TITLE, schoolAgedRows));
    headcountTableDataList.add(buildHeadcountTableData(ADULT_TITLE, adultRows));
    headcountTableDataList.add(buildHeadcountTableData(TOTAL_TITLE, totalRows));

    return headcountTableDataList;
  }

  private HeadCountTableDataRow calculateSummaryRow(List<HeadCountTableDataRow> rows) {
    Long summaryHeadcount = rows.stream().mapToLong(row -> Long.parseLong(row.getColumnTitleAndValueMap().get(HEADCOUNT_TITLE))).sum();
    Long summaryEligibleForFTE = rows.stream().mapToLong(row -> Long.parseLong(row.getColumnTitleAndValueMap().get(ELIGIBLE_FTE_TITLE))).sum();
    BigDecimal summaryFteTotal = rows.stream().map(row -> new BigDecimal(row.getColumnTitleAndValueMap().get(TOTAL_FTE_TITLE))).reduce(BigDecimal.ZERO, BigDecimal::add);

    Map<String, String> valuesMap = new HashMap<>();

    valuesMap.put(HEADCOUNT_TITLE, String.valueOf(summaryHeadcount));
    valuesMap.put(ELIGIBLE_FTE_TITLE, String.valueOf(summaryEligibleForFTE));
    valuesMap.put(TOTAL_FTE_TITLE, String.valueOf(summaryFteTotal));

    return HeadCountTableDataRow.builder()
            .title(TOTAL_TITLE)
            .columnTitleAndValueMap(valuesMap)
            .build();
  }

  private HeadCountTableDataRow buildDataRow(EnrollmentHeadcountResult result, String title, String gradeCode) {
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
        case TOTAL_TITLE -> {
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

  private HeadcountTableData buildHeadcountTableData(String title, List<HeadCountTableDataRow> rows) {
    List<String> columnNames = new ArrayList<>();
    columnNames.add(HEADCOUNT_TITLE);
    columnNames.add(ELIGIBLE_FTE_TITLE);
    columnNames.add(TOTAL_FTE_TITLE);

    return HeadcountTableData.builder()
            .title(title)
            .columnNames(columnNames)
            .rows(rows)
            .build();
  }
}
