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
        int age = LocalDate.now().getYear() - LocalDate.parse(dob, format).getYear();
        return age >= 19;
    }

    public static boolean isSchoolAged(String dob) {
        int age = LocalDate.now().getYear() - LocalDate.parse(dob, format).getYear();
        return age >= 5 && age < 19;
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
