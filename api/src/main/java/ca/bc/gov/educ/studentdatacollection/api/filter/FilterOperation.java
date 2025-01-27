package ca.bc.gov.educ.studentdatacollection.api.filter;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Optional;

public enum FilterOperation {

  /**
   * Equal filter operation.
   */
  EQUAL("eq"),
  /**
   * Equal Other Field filter operation.
   */
  NOT_EQUAL_OTHER_COLUMN("neqc"),
  /**
   * Not equal filter operation.
   */
  NOT_EQUAL("neq"),
  /**
   * Greater than filter operation.
   */
  GREATER_THAN("gt"),
  /**
   * Greater than or equal to filter operation.
   */
  GREATER_THAN_OR_EQUAL_TO("gte"),
  /**
   * Less than filter operation.
   */
  LESS_THAN("lt"),
  /**
   * Less than or equal to filter operation.
   */
  LESS_THAN_OR_EQUAL_TO("lte"),
  /**
   * In filter operation.
   */
  IN("in"),
  /**
   * Filter to return when none of the child records includes the values
   */
  NONE_IN("none_in"),
  NONE_IN_DISTRICT("none_in_district"),
  /**
   * Not in filter operation.
   */
  NOT_IN("nin"),
  /**
   * Between filter operation.
   */
  BETWEEN("btn"),
  /**
   * Contains filter operation.
   */
  CONTAINS("like"),
  /**
   * Starts with filter operation.
   */
  STARTS_WITH("starts_with"),
  /**
   * Not Starts with filter operation.
   */
  NOT_STARTS_WITH("not_starts_with"),
  /**
   * Ends with filter operation.
   */
  ENDS_WITH("ends_with"),
  /**
   * Starts with ignore case filter operation.
   */
  STARTS_WITH_IGNORE_CASE("starts_with_ignore_case"),
  /**
   * Contains ignore case filter operation.
   */
  CONTAINS_IGNORE_CASE("like_ignore_case"),
  IN_LEFT_JOIN("in_left_join"),
  IN_NOT_DISTINCT("in_not_distinct");

  private final String value;

  FilterOperation(String value) {
    this.value = value;
  }

  @Override
  @JsonValue
  public String toString() {
    return String.valueOf(value);
  }

  public static Optional<FilterOperation> fromValue(String value) {
    for (FilterOperation op : FilterOperation.values()) {
      if (String.valueOf(op.value).equalsIgnoreCase(value)) {
        return Optional.of(op);
      }
    }
    return Optional.empty();
  }

}
