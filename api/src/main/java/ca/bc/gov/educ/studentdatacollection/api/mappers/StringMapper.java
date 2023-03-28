package ca.bc.gov.educ.studentdatacollection.api.mappers;

import org.apache.commons.lang3.StringUtils;

/**
 * The type String mapper.
 */
public class StringMapper {

  private StringMapper() {
  }

  /**
   * Map string.
   *
   * @param value the value
   * @return the string
   */
  public static String map(final String value) {
    if (StringUtils.isNotBlank(value)) {
      return value.trim();
    }
    return value;
  }

  public static Long getLongValueFromString(String s){
    if(StringUtils.isEmpty(s) && StringUtils.isNumeric(s)){
      return Long.getLong(s);
    }
    return null;
  }

  public static String trimUppercaseAndScrubDiacriticalMarks(String value){
    if (StringUtils.isNotBlank(value)) {
      return StringUtils.stripAccents(StringUtils.trim(value)).replaceAll("[^\\p{ASCII}]", "¿").toUpperCase();
    }
    return value;
  }
}
