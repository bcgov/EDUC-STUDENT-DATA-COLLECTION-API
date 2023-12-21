package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

@Component
@Slf4j
public class HeadcountHelper<T extends HeadcountResult> {
  protected final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  protected final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  protected Map<String, Function<T, String>> headcountMethods;
  protected Map<String, String> sectionTitles;
  protected Map<String, String> rowTitles;
  protected List<String> gradeCodes;

  public HeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
    this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
    this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
  }

  public void setComparisonValues(List<HeadcountHeader> headcountHeaderList, List<HeadcountHeader> previousHeadcountHeaderList) {
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

  public UUID getPreviousSeptemberCollectionID(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
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

  public void stripZeroColumns(HeadcountHeader headcountHeader) {
    Map<String, HeadcountHeaderColumn> map = headcountHeader.getColumns();
    List<String> newOrderedTitles = new ArrayList<>();

    headcountHeader.getOrderedColumnTitles().forEach(title -> {
      var currentValueIsZero = StringUtils.isBlank(map.get(title).getCurrentValue()) || map.get(title).getCurrentValue().equals("0");
      var comparisonValueIsZero = StringUtils.isBlank(map.get(title).getComparisonValue()) || map.get(title).getComparisonValue().equals("0");
      if (currentValueIsZero && comparisonValueIsZero) {
        headcountHeader.getColumns().remove(title);
      } else {
        newOrderedTitles.add(title);
      }
    });
    headcountHeader.setOrderedColumnTitles(newOrderedTitles);
  }

  public HeadcountResultsTable convertHeadcountResults(List<T> results) {
    HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
    List<String> columnTitles = new ArrayList<>(gradeCodes);
    columnTitles.add(0, "title");
    columnTitles.add("Total");
    headcountResultsTable.setHeaders(columnTitles);
    headcountResultsTable.setRows(new ArrayList<>());

    List<Map<String, String>> rows = new ArrayList<>();
    for (Map.Entry<String, String> title : rowTitles.entrySet()) {
      Map<String, String> rowData = new LinkedHashMap<>();
      rowData.put("title", title.getValue());
      BigDecimal total = BigDecimal.ZERO;

      Function<T, String> headcountFunction = headcountMethods.get(title.getKey());
      String section = sectionTitles.get(title.getKey());
      if(headcountFunction != null) {
        for (String gradeCode : gradeCodes) {
          var result = results.stream()
                  .filter(value -> value.getEnrolledGradeCode().equals(gradeCode))
                  .findFirst()
                  .orElse(null);
            String headcount = "0";
            if (result != null && result.getEnrolledGradeCode().equals(gradeCode)) {
              headcount = headcountFunction.apply(result);
            }
            rowData.put(gradeCode, headcount);
            total = total.add(new BigDecimal(headcount));
        }
        rowData.put("Total", String.valueOf(total));
      }
      rowData.put("section", section);
      rows.add(rowData);
    }
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }
}
