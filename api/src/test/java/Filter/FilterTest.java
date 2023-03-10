package Filter;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import org.junit.jupiter.api.Test;

public class FilterTest {

  @Test
  void filterOperationShouldReturnCorrectValues() {
    String[] expectedValues = {"eq", "neq", "gt", "gte", "lt", "lte", "in", "nin", "like",
        "like_ignore_case", "starts_with", "starts_with_ignore_case"};
    String[] actualValues = {
        FilterOperation.EQUAL.toString(),
        FilterOperation.NOT_EQUAL.toString(),
        FilterOperation.GREATER_THAN.toString(),
        FilterOperation.GREATER_THAN_OR_EQUAL_TO.toString(),
        FilterOperation.LESS_THAN.toString(),
        FilterOperation.LESS_THAN_OR_EQUAL_TO.toString(),
        FilterOperation.IN.toString(),
        FilterOperation.NOT_IN.toString(),
        FilterOperation.CONTAINS.toString(),
        FilterOperation.CONTAINS_IGNORE_CASE.toString(),
        FilterOperation.STARTS_WITH.toString(),
        FilterOperation.STARTS_WITH_IGNORE_CASE.toString()};

    assertArrayEquals(expectedValues, actualValues);
  }

  @Test
  void filterOperationFromValue_GivenValidValue_ShouldReturnCorrectFilterOperation() {
    assertEquals(FilterOperation.EQUAL ,FilterOperation.fromValue("eq").get());
  }
}
