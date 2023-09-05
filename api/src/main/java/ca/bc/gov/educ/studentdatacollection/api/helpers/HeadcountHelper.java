package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HeadCountTableDataRow;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HeadcountTableData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.stream.IntStream;

@Component
@Slf4j
public class HeadcountHelper {
  protected final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  protected final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

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

  public HeadCountTableDataRow calculateSummaryRow(List<HeadCountTableDataRow> rows, String[] keys, String rowTitle) {
    Map<String, String> summaryMap = new HashMap<>();
    String fteTotalTitle = "FTE Total";
    for (String key : keys) {
      if(key.equals(fteTotalTitle)) {
        BigDecimal valueSum = rows.stream().map(row -> new BigDecimal(row.getColumnTitleAndValueMap().get(fteTotalTitle))).reduce(BigDecimal.ZERO, BigDecimal::add);
        summaryMap.put(key, String.valueOf(valueSum));
      } else {
        Long valueSum = rows.stream().mapToLong(row -> Long.parseLong(row.getColumnTitleAndValueMap().get(key))).sum();
        summaryMap.put(key, String.valueOf(valueSum));
      }
    }
    return HeadCountTableDataRow.builder().title(rowTitle).columnTitleAndValueMap(summaryMap).build();
  }
  public HeadcountTableData buildHeadcountTableData(String title, List<HeadCountTableDataRow> rows, List<String> columnNames) {
    return HeadcountTableData.builder().title(title).columnNames(columnNames).rows(rows).build();
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
}
