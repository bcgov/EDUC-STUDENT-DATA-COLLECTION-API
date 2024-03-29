package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentValidationIssueEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.format.SignStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static java.time.temporal.ChronoField.*;

public final class SdcHelper {
  public static final DateTimeFormatter YYYY_MM_DD_SLASH_FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendLiteral("/")
    .appendValue(MONTH_OF_YEAR, 2)
    .appendLiteral("/")
    .appendValue(DAY_OF_MONTH, 2).toFormatter();
  public static final DateTimeFormatter YYYY_MM_DD_FORMATTER = new DateTimeFormatterBuilder()
    .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
    .appendValue(MONTH_OF_YEAR, 2)
    .appendValue(DAY_OF_MONTH, 2).toFormatter();

  private SdcHelper() {

  }

  public static Optional<LocalDate> getBirthDateFromString(final String birthDate) {
    try {
      return Optional.of(LocalDate.parse(birthDate)); // yyyy-MM-dd
    } catch (final DateTimeParseException dateTimeParseException) {
      try {
        return Optional.of(LocalDate.parse(birthDate, YYYY_MM_DD_SLASH_FORMATTER));// yyyy/MM/dd
      } catch (final DateTimeParseException dateTimeParseException2) {
        try {
          return Optional.of(LocalDate.parse(birthDate, YYYY_MM_DD_FORMATTER));// yyyyMMdd
        } catch (final DateTimeParseException dateTimeParseException3) {
          return Optional.empty();
        }
      }
    }
  }

  /**
   * @param date the string date to be validated.
   * @return true if it is yyyy-MM-dd format false otherwise.
   */
  public static boolean isValidDate(final String date) {
    try {
      LocalDate.parse(date);
      return true;
    } catch (final DateTimeParseException dateTimeParseException3) {
      return false;
    }
  }

  public static Set<SdcSchoolCollectionStudentValidationIssueEntity> populateValidationErrors(final List<SdcSchoolCollectionStudentValidationIssue> issues, final SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity) {
    final Set<SdcSchoolCollectionStudentValidationIssueEntity> validationErrors = new HashSet<>();
    issues.forEach(issue -> {
      final SdcSchoolCollectionStudentValidationIssueEntity error = new SdcSchoolCollectionStudentValidationIssueEntity();
      error.setValidationIssueFieldCode(issue.getValidationIssueFieldCode());
      error.setValidationIssueSeverityCode(issue.getValidationIssueSeverityCode());
      error.setValidationIssueCode(issue.getValidationIssueCode());
      error.setSdcSchoolCollectionStudentEntity(sdcSchoolStudentEntity);
      error.setCreateDate(LocalDateTime.now());
      error.setUpdateDate(LocalDateTime.now());
      error.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      error.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      validationErrors.add(error);
    });
    return validationErrors;
  }

  public static String removeLeadingZeros(final String federalBandCode) {
    if (StringUtils.isBlank(federalBandCode)) {
      return "";
    } else {
      return federalBandCode.replaceFirst("^0+(?!$)", "");
    }
  }
}
