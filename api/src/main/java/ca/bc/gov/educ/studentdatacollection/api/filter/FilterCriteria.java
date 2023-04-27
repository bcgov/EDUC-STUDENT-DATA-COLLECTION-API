package ca.bc.gov.educ.studentdatacollection.api.filter;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

/**
 * Filter Criteria Holder
 *
 * @param <T> is the java type of the DB table column
 * @author om
 */
public class FilterCriteria<T extends Comparable<T>> {

  /**
   * Holds the operation {@link FilterOperation}
   */
  private final FilterOperation operation;

  /**
   * Table column name
   */
  private final String fieldName;

  /**
   * Holds the Function to convertString to <T>
   */
  private final Function<String, T> converterFunction;

  /**
   * Converted value
   */
  private T convertedSingleValue;

  /**
   * minimum value - application only for {@link FilterOperation#BETWEEN}
   */
  private T minValue;

  /**
   * maximum value - application only for {@link FilterOperation#BETWEEN}
   */
  private T maxValue;

  /**
   * Holds the filter criteria
   */
  private final Collection<String> originalValues;

  /**
   * Holds the filter criteria as type <T>
   */
  private final Collection<T> convertedValues;

  public FilterCriteria(@NotNull String fieldName, @NotNull String fieldValue, @NotNull FilterOperation filterOperation, Function<String, T> converterFunction) {

    this.fieldName = fieldName;
    this.converterFunction = converterFunction;

    // Split the fieldValue value as comma separated.
    String[] operationValues = StringUtils.split(fieldValue, ",");

    if (operationValues.length < 1) {
      throw new IllegalArgumentException("field value can't be empty");
    }
    this.operation = filterOperation;
    this.originalValues = Arrays.asList(operationValues);
    this.convertedValues = new ArrayList<>();

    // Validate other conditions
    validateAndAssign(operationValues);

  }

  private void validateAndAssign(String[] operationValues) {

    //For operation 'btn'
    if (FilterOperation.BETWEEN == operation) {
      if (operationValues.length != 2) {
        throw new IllegalArgumentException("For 'btn' operation two values are expected");
      } else {

        //Convert
        T value1 = this.converterFunction.apply(operationValues[0]);
        T value2 = this.converterFunction.apply(operationValues[1]);

        //Set min and max values
        if (value1.compareTo(value2) > 0) {
          this.minValue = value2;
          this.maxValue = value1;
        } else {
          this.minValue = value1;
          this.maxValue = value2;
        }
      }

      //For 'in' or 'nin' operation
    } else if (FilterOperation.IN == operation || FilterOperation.NOT_IN == operation) {
      convertedValues.addAll(originalValues.stream().map(converterFunction).toList());
    } else {
      //All other operation
      this.convertedSingleValue = converterFunction.apply(operationValues[0]);
    }

  }

  public T getConvertedSingleValue() {
    return convertedSingleValue;
  }

  public T getMinValue() {
    return minValue;
  }

  public T getMaxValue() {
    return maxValue;
  }

  public FilterOperation getOperation() {
    return operation;
  }

  public String getFieldName() {
    return fieldName;
  }

  public Function<String, T> getConverterFunction() {
    return converterFunction;
  }

  public Collection<String> getOriginalValues() {
    return originalValues;
  }

  public Collection<T> getConvertedValues() {
    return convertedValues;
  }

}
