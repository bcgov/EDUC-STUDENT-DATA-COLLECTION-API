package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

@Component
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

  public void setResultsTableComparisonValues(HeadcountResultsTable collectionData, HeadcountResultsTable previousCollectionData) {
    List<Map<String, HeadcountHeaderColumn>> rows = collectionData.getRows();
    IntStream.range(0, rows.size())
            .forEach(i -> {
              var  currentData = collectionData.getRows().get(i);
              var previousData = previousCollectionData.getRows().get(i);

              currentData.forEach((rowName, currentRow) -> {
                HeadcountHeaderColumn previousColumn = previousData.get(rowName);
                currentRow.setComparisonValue(previousColumn.getCurrentValue());
              });
            });
  }

  public UUID getPreviousSeptemberCollectionID(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    var septemberCollection = sdcSchoolCollectionRepository.findLastCollectionByType(sdcSchoolCollectionEntity.getSchoolID(), CollectionTypeCodes.SEPTEMBER.getTypeCode(), sdcSchoolCollectionEntity.getSdcSchoolCollectionID());
    if(septemberCollection.isPresent()) {
      return septemberCollection.get().getSdcSchoolCollectionID();
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

    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
    for (Map.Entry<String, String> title : rowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();

      rowData.put("title", HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
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
            rowData.put(gradeCode, HeadcountHeaderColumn.builder().currentValue(headcount).build());
            total = total.add(new BigDecimal(headcount));
        }
        rowData.put("Total", HeadcountHeaderColumn.builder().currentValue(String.valueOf(total)).build());
      }
      rowData.put("section", HeadcountHeaderColumn.builder().currentValue(section).build());
      rows.add(rowData);
    }
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }
}
