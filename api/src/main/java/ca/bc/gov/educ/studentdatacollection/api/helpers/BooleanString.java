package ca.bc.gov.educ.studentdatacollection.api.helpers;
import org.apache.commons.lang3.StringUtils;

/**
 * A static utility class for comparing Boolean strings while being flexible with the null/empty value case.
 */
public final class BooleanString {

  private static void booleanStringMustBeTrueOrFalse(String val) {
    if (!val.equals("true") && !val.equals("false")) {
      throw new IllegalArgumentException("Boolean string must be a value of \"true\" or \"false\"");
    }
  }

  /**
   * Compare a strings that represent a boolean value to a Boolean and see if they are the same.
   *
   * @param testValue The string being tested. If it's empty, it will be considered falsy.
   * @param trueOrFalse Must be a string of "true" or "false"
   */
  public static boolean areEqual(String testValue, Boolean trueOrFalse) {
    return StringUtils.isNotEmpty(testValue) && testValue.equals(trueOrFalse.toString());
  }

  /**
   * Compare two strings that represent a boolean value to a each-other and see if they are the same.
   *
   * @param testValue The string being tested. If it's empty, it will be considered falsy.
   * @param trueOrFalse Must be a string of "true" or "false"
   */
  public static boolean areEqual(String testValue, String trueOrFalse) {
    booleanStringMustBeTrueOrFalse(trueOrFalse);
    return areEqual(testValue, Boolean.valueOf(trueOrFalse));
  }

  /**
   * Compare two strings that represent a boolean value to see if they are the same, with the option to coerce
   * empty values into "truthy" or "falsy" values.
   *
   * @param testValue The string being tested
   * @param trueOrFalse Must be a string of "true" or "false"
   * @param defaultForNull A boolean default for value an empty testValue
   */
  public static boolean areEqual(String testValue, Boolean trueOrFalse, boolean defaultForNull) {
    if (StringUtils.isEmpty(testValue)) {
      if (defaultForNull == true) {
        return areEqual(Boolean.TRUE.toString(), trueOrFalse);
      }
      return areEqual(Boolean.FALSE.toString(), trueOrFalse);
    }

    return areEqual(testValue, trueOrFalse);
  }

  /**
   * Compare two strings that represent a boolean value to see if they are the same, with the option to coerce
   * empty values into "truthy" or "falsy" values.
   *
   * @param testValue The string being tested
   * @param trueOrFalse Must be a string of "true" or "false"
   * @param defaultForNull A boolean default for value an empty testValue
   */
  public static boolean areEqual(String testValue, String trueOrFalse, boolean defaultForNull) {
    booleanStringMustBeTrueOrFalse(trueOrFalse);
    return areEqual(testValue, Boolean.valueOf(trueOrFalse), defaultForNull);
  }

}
