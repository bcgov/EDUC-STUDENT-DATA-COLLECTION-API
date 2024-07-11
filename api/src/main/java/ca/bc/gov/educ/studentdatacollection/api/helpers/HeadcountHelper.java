package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class HeadcountHelper<T extends HeadcountResult> {
  protected final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
  protected final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  protected final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  protected Map<String, Function<T, String>> headcountMethods;
  protected Map<String, String> sectionTitles;
  protected Map<String, String> rowTitles;
  protected List<String> gradeCodes;

  private static final String TITLE = "title";
  public static final String SECTION="section";

  public HeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
      this.sdcSchoolCollectionRepository = sdcSchoolCollectionRepository;
      this.sdcSchoolCollectionStudentRepository = sdcSchoolCollectionStudentRepository;
      this.sdcDistrictCollectionRepository = sdcDistrictCollectionRepository;
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
              boolean previousCollectionHasData = i < previousCollectionData.getRows().size();
              if(previousCollectionHasData) {
                var previousData = previousCollectionData.getRows().get(i);
                currentData.forEach((rowName, currentRow) -> {
                  HeadcountHeaderColumn previousColumn = previousData.get(rowName);
                  currentRow.setComparisonValue(previousColumn.getCurrentValue());
                });
              } else {
                currentData.forEach((rowName, currentRow) -> currentRow.setComparisonValue("0"));
              }
            });
  }

  public void setResultsTableComparisonValuesDynamic(HeadcountResultsTable currentCollectionData, HeadcountResultsTable previousCollectionData) {
    Map<String, Map<String, HeadcountHeaderColumn>> previousRowsMap = previousCollectionData.getRows().stream()
            .filter(row -> row.containsKey(TITLE) && row.get(TITLE) != null)
            .collect(Collectors.toMap(
                    row -> row.get(TITLE).getCurrentValue(),
                    Function.identity(),
                    (existing, replacement) -> existing
            ));

    Map<String, Map<String, HeadcountHeaderColumn>> allTitles = new LinkedHashMap<>();

    currentCollectionData.getRows().forEach(row -> allTitles.put(row.get(TITLE).getCurrentValue(), row));
    previousRowsMap.keySet().forEach(title -> allTitles.putIfAbsent(title, new HashMap<>()));

    for (Map.Entry<String, Map<String, HeadcountHeaderColumn>> entry : allTitles.entrySet()) {
      String title = entry.getKey();
      Map<String, HeadcountHeaderColumn> currentRow = entry.getValue();
      Map<String, HeadcountHeaderColumn> previousRow = previousRowsMap.getOrDefault(title, new HashMap<>());

      currentRow.forEach((key, currentColumn) -> {
        if (previousRow.containsKey(key)) {
          currentColumn.setComparisonValue(previousRow.get(key).getCurrentValue());
        } else {
          currentColumn.setComparisonValue("0");
        }
      });

      previousRow.keySet().forEach(columnKey -> {
        currentRow.putIfAbsent(columnKey, new HeadcountHeaderColumn("0", "0"));
        currentRow.get(columnKey).setComparisonValue(previousRow.get(columnKey).getCurrentValue());
      });
    }
  }

  public void setResultsTableComparisonValuesDynamicNested(HeadcountResultsTable currentCollectionData, HeadcountResultsTable previousCollectionData) {
    Map<String, Map<String, HeadcountHeaderColumn>> previousRowsMap = previousCollectionData.getRows().stream()
            .filter(row -> row.containsKey(SECTION) && row.get(SECTION) != null && row.containsKey(TITLE))
            .collect(Collectors.toMap(
                    row -> row.get(SECTION).getCurrentValue() + "-" + row.get(TITLE).getCurrentValue(),
                    Function.identity(),
                    (existing, replacement) -> existing
            ));

    Map<String, Map<String, HeadcountHeaderColumn>> allTitles = new LinkedHashMap<>();

    currentCollectionData.getRows().forEach(row -> {
      if (row.containsKey(SECTION) && row.get(SECTION) != null && row.containsKey(TITLE)) {
        String key = row.get(SECTION).getCurrentValue() + "-" + row.get(TITLE).getCurrentValue();
        allTitles.put(key, row);
      }
    });

    allTitles.forEach((key, currentRow) -> {
      Map<String, HeadcountHeaderColumn> previousRow = previousRowsMap.getOrDefault(key, new HashMap<>());
      currentRow.forEach((columnKey, currentColumn) -> {
        if (previousRow.containsKey(columnKey)) {
          currentColumn.setComparisonValue(previousRow.get(columnKey).getCurrentValue());
        } else {
          currentColumn.setComparisonValue("0");
        }
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

  public UUID getPreviousSeptemberCollectionIDByDistrictCollectionID(SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
    var septemberCollection = sdcDistrictCollectionRepository.findLastCollectionByType(sdcDistrictCollectionEntity.getDistrictID(), CollectionTypeCodes.SEPTEMBER.getTypeCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionID());
      return septemberCollection.map(SdcDistrictCollectionEntity::getSdcDistrictCollectionID).orElse(null);
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

  public void stripPreSchoolSection(HeadcountResultsTable collectionData) {
    List<Map<String, HeadcountHeaderColumn>> newRows = collectionData.getRows().stream().filter(row -> !row.get(SECTION).getCurrentValue().equalsIgnoreCase("Preschool Aged")).toList();
    collectionData.setRows(newRows);
  }

  public HeadcountResultsTable convertHeadcountResults(List<T> results) {
    HeadcountResultsTable headcountResultsTable = new HeadcountResultsTable();
    List<String> columnTitles = new ArrayList<>(gradeCodes);
    columnTitles.add(0, TITLE);
    columnTitles.add("Total");
    headcountResultsTable.setHeaders(columnTitles);
    headcountResultsTable.setRows(new ArrayList<>());

    List<Map<String, HeadcountHeaderColumn>> rows = new ArrayList<>();
    for (Map.Entry<String, String> title : rowTitles.entrySet()) {
      Map<String, HeadcountHeaderColumn> rowData = new LinkedHashMap<>();

      rowData.put(TITLE, HeadcountHeaderColumn.builder().currentValue(title.getValue()).build());
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
      rowData.put(SECTION, HeadcountHeaderColumn.builder().currentValue(section).build());
      rows.add(rowData);
    }
    headcountResultsTable.setRows(rows);
    return headcountResultsTable;
  }
}
