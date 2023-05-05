package ca.bc.gov.educ.studentdatacollection.api.util;

import java.time.LocalDate;

public class SchoolYear {
  private LocalDate startDate;
  private LocalDate endDate;

  public SchoolYear() {
    final LocalDate dateNow = LocalDate.now();
    final Integer currentMonth = dateNow.getMonthValue();
    final Integer schoolYear = (currentMonth < 7) ? dateNow.getYear() - 1 : dateNow.getYear();

    startDate = LocalDate.of(schoolYear, 7, 1);
    endDate = LocalDate.of(schoolYear + 1, 6, 30);
  }

  public SchoolYear(Integer startYear) {
    startDate = LocalDate.of(startYear, 7, 1);
    endDate = LocalDate.of(startYear + 1, 6, 30);
  }

  public SchoolYear(String startYear) {
    this(Integer.parseInt(startYear));
  }

  public LocalDate getStartDate() { return startDate; }
  public LocalDate getEndDate() { return endDate; }
}
