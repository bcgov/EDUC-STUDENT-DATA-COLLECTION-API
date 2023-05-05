package ca.bc.gov.educ.studentdatacollection.api.helpers;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;

public class SdcHelperTest {
  private void testAbstractSchoolYearRange(LocalDate now) throws AssertionError {
    final Integer currentMonth = now.getMonthValue();
    final Integer currentYear = now.getYear();

    Pair<LocalDate, LocalDate> currentSchoolYear = SdcHelper.getFirstAndLastDatesOfSchoolYear();
    if (currentMonth < 7) {
      assertEquals((currentYear - 1) + "-07-01", currentSchoolYear.getLeft().toString());
      assertEquals(currentYear + "-06-30", currentSchoolYear.getRight().toString());
    } else {
      assertEquals(currentYear + "-07-01", currentSchoolYear.getLeft().toString());
      assertEquals((currentYear + 1) + "-06-30", currentSchoolYear.getRight().toString());
    }
  }

  @Test
  public void testGetFirstAndLastDatesOfSchoolYear() throws AssertionError {
    Pair<LocalDate, LocalDate> schoolYear = SdcHelper.getFirstAndLastDatesOfSchoolYear(2023);
    assertEquals("2023-07-01", schoolYear.getLeft().toString());
    assertEquals("2024-06-30", schoolYear.getRight().toString());

    Pair<LocalDate, LocalDate> schoolYearStrFromStr = SdcHelper
      .getFirstAndLastDatesOfSchoolYear("2023");
    assertEquals("2023-07-01", schoolYearStrFromStr.getLeft().toString());
    assertEquals("2024-06-30", schoolYearStrFromStr.getRight().toString());

    final LocalDate dateNow = LocalDate.now();
    testAbstractSchoolYearRange(dateNow);
    testAbstractSchoolYearRange(dateNow.minusMonths(6));
  }
}
