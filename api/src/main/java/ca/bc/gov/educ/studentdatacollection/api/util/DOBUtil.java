package ca.bc.gov.educ.studentdatacollection.api.util;

import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

public class DOBUtil {

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("uuuuMMdd").withResolverStyle(ResolverStyle.STRICT);

    private DOBUtil() {
    }

    public static int getStudentAge(String dob) {
        return LocalDate.now().getYear() - LocalDate.parse(dob, format).getYear();
    }

    public static boolean isAdult(String dob) {
        SchoolYear schoolYear = new SchoolYear();
        LocalDate dateStudentIsAdult = LocalDate.parse(dob, format).plusYears(19);
        return dateStudentIsAdult.isBefore(schoolYear.getStartDate());
    }

    public static boolean isSchoolAged(String dob) {
        SchoolYear schoolYear = new SchoolYear();
        LocalDate decemberCutoff = schoolYear.getStartDate().plusMonths(6).plusDays(30);
        LocalDate dateStudentTurnedFive = LocalDate.parse(dob, format).plusYears(5);
        return dateStudentTurnedFive.isBefore(decemberCutoff) && !isAdult(dob);
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
