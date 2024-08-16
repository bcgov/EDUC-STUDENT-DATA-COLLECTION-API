package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.SignStyle;
import java.util.Optional;

import static java.time.temporal.ChronoField.*;

/**
 * The type Local date time util.
 */
public final class LocalDateTimeUtil {

  /**
   * Instantiates a new Local date time util.
   */
  private LocalDateTimeUtil() {
  }

  /**
   * Get api formatted date of birth string.
   *
   * @param dateOfBirth the date of birth
   * @return the string
   */
  public static String getAPIFormattedDateOfBirth(String dateOfBirth) {
    if (Optional.ofNullable(dateOfBirth).isEmpty()) {
      return null;
    }
    DateTimeFormatter dateTimeFormatter = new DateTimeFormatterBuilder()
        .appendValue(YEAR, 4, 10, SignStyle.EXCEEDS_PAD)
        .appendValue(MONTH_OF_YEAR, 2)
        .appendValue(DAY_OF_MONTH, 2).toFormatter();
    LocalDate date = LocalDate.parse(dateOfBirth, dateTimeFormatter);
    return date.format(DateTimeFormatter.ISO_DATE);
  }

  public static String getSchoolYearString(CollectionEntity collection){
    var snapshotDateString = collection.getSnapshotDate();
    if(!collection.getCollectionTypeCode().equals(CollectionTypeCodes.SEPTEMBER.getTypeCode())){
      return snapshotDateString.minusYears(1).getYear() + "/" + snapshotDateString.getYear() + "SY";
    }else{
      return snapshotDateString.getYear() + "/" + snapshotDateString.plusYears(1).getYear() + "SY";
    }
  }
}
