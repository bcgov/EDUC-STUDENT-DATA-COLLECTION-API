package ca.bc.gov.educ.studentdatacollection.api.mappers;

import org.apache.commons.lang3.StringUtils;

public class StringMapper {

  private StringMapper() {
  }

  public static String map(final String value) {
    if (StringUtils.isNotBlank(value)) {
      return value.trim();
    }
    return value;
  }

  public static String trimAndUppercase(String value){
    if (StringUtils.isNotBlank(value)) {
      return StringUtils.trim(value).toUpperCase();
    }
    return value;
  }

  public static String removeLeadingApostrophes(String value) {
    if (StringUtils.isNotBlank(value)) {
      value = value.trim();
      if (value.startsWith("'") && (!StringUtils.isAlpha(value.substring((1)))) ) {
        return value.substring(1);
      }
    }
    return value;
  }


  public static String processGivenName(String value) {
    String legalGivenName = removeLeadingApostrophes(value);
    return trimAndUppercase(legalGivenName);
  }

  public static void main(String[] args) {
    String[] testCases = {
            "'",       // Expected output: ""
            "''",      // Expected output: ""
            "'Jackson",// Expected output: "'Jackson"
            " Space 'Jackson", // Expected output: "'Jackson"
            "MC'Onnel",
            "    'JAckson", // Expected output: "'JAckson"
    };

    for (String testCase : testCases) {
      String result = removeLeadingApostrophes(testCase);
      System.out.println(result);
    }
  }

}
