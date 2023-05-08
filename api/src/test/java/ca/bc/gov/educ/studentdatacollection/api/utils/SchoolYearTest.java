package ca.bc.gov.educ.studentdatacollection.api.utils;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ca.bc.gov.educ.studentdatacollection.api.util.SchoolYear;

class SchoolYearTest {
  private void testAbstractSchoolYearRange(LocalDate now) throws AssertionError {
    final Integer currentMonth = now.getMonthValue();
    final Integer currentYear = now.getYear();

    SchoolYear currentSchoolYear = new SchoolYear();
    if (currentMonth < 7) {
      assertEquals((currentYear - 1) + "-07-01", currentSchoolYear.getStartDate().toString());
      assertEquals(currentYear + "-06-30", currentSchoolYear.getEndDate().toString());
    } else {
      assertEquals(currentYear + "-07-01", currentSchoolYear.getStartDate().toString());
      assertEquals((currentYear + 1) + "-06-30", currentSchoolYear.getEndDate().toString());
    }
  }

  @Test
  void testGetFirstAndLastDatesOfSchoolYear() throws AssertionError {
    SchoolYear schoolYear = new SchoolYear(2023);
    assertEquals("2023-07-01", schoolYear.getStartDate().toString());
    assertEquals("2024-06-30", schoolYear.getEndDate().toString());

    SchoolYear schoolYearStrFromStr = new SchoolYear("2023");
    assertEquals("2023-07-01", schoolYearStrFromStr.getStartDate().toString());
    assertEquals("2024-06-30", schoolYearStrFromStr.getEndDate().toString());

    final LocalDate dateNow = LocalDate.now();
    testAbstractSchoolYearRange(dateNow);
    testAbstractSchoolYearRange(dateNow.minusMonths(6));
  }
}
