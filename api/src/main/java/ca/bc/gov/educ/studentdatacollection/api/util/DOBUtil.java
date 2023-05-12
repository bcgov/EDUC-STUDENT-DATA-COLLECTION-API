package ca.bc.gov.educ.studentdatacollection.api.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class DOBUtil {

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private DOBUtil() {
    }

    public static int getStudentAge(String dob) {
        return Period.between(LocalDate.parse(dob, format), LocalDate.now()).getYears();
    }

    public static boolean isAdult(String dob) {
        SchoolYear schoolYear = new SchoolYear();
        LocalDate dateStudentIsAdult = LocalDate.parse(dob, format).plusYears(19);
        return dateStudentIsAdult.isBefore(schoolYear.getStartDate());
    }

    public static boolean is5YearsOldByDec31ThisSchoolYear(String dob) {
        SchoolYear schoolYear = new SchoolYear();
        LocalDate decemberCutoff = LocalDate.parse(schoolYear.getStartDate().getYear() + "-12-31");
        LocalDate dateStudentTurnedFive = LocalDate.parse(dob, format).plusYears(5);
        return dateStudentTurnedFive.isBefore(decemberCutoff);
    }

    public static boolean isSchoolAged(String dob) {
       return is5YearsOldByDec31ThisSchoolYear(dob) && !isAdult(dob);
    }

    public static boolean isValidDate(String dob) {
        if (StringUtils.isEmpty(dob)) {
            return false;
        }
        try {
            LocalDate.parse(dob, format);
        } catch (DateTimeParseException ex) {
            return false;
        }
        return true;
    }
}
