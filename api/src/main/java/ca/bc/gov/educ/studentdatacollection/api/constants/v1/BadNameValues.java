package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import java.util.Arrays;
import java.util.Optional;

public enum BadNameValues {

    BLANK("BLANK"),
    DATA("DATA"),
    DOESNT_HAVE_ONE("DOESN'T HAVE ONE"),
    DOESN0T_HAVE_ONE("DOESNT HAVE ONE"),
    DONT_KNOW("DON'T KNOW"),
    DONOT_KNOW("DONT KNOW"),
    DUMMY("DUMMY"),
    DUPLICATE("DUPLICATE"),
    MIDDLE("MIDDLE"),
    N_A("N A"),
    NA("NA"),
    NOT_AVAILABLE("NOT AVAILABLE"),
    NOTAVAILABLE("NOTAVAILABLE"),
    NOTAPPLICABLE("NOTAPPLICABLE"),
    NON_APPLICABLE("NON APPLICABLE"),
    NONHAPPLICABLE("NON-APPLICABLE"),
    NONAPPLICABLE("NONAPPLICABLE"),
    NOT_APPLICABLE("NOT APPLICABLE"),
    NONE("NONE"),
    NOT("NOT"),
    NMN("NMN"),
    NOMIDDLENAME("NOMIDDLENAME"),
    SAME("SAME"),
    SAMPLE("SAMPLE"),
    STUDENT("STUDENT"),
    TESTSTUD("TESTSTUD"),
    TEST("TEST"),
    TESTING("TESTING"),
    UNKNOWN("UNKNOWN"),
    NO_SURNAME("NO SURNAME"),
    NOSURNAME("NOSURNAME"),
    UNAVAILABLE("UNAVAILABLE"),
    ABC("ABC"),
    MISSING("MISSING"),
    ASD("ASD"),
    LAST_NAME_BLANK("LAST NAME BLANK"),
    TESTHSTUDENT("TEST-STUDENT"),
    TESTFILE("TESTFILE"),
    TESTTEST("TESTTEST"),
    NO_LAST_NAME("NO LAST NAME"),
    RECORD("RECORD"),
    INACTIVE("INACTIVE"),
    FIRST_NAME("FIRST NAME"),
    TESTONLY("TESTONLY"),
    TESTER("TESTER"),
    TESTSTUDENT("TESTSTUDENT"),
    NULL("NULL"),
    TESTFIRSTNAME("TESTFIRSTNAME"),
    TESTLASTNAME("TESTLASTNAME"),
    DO_NOT_USE("DO NOT USE"),
    DELETE("DELETE"),
    DONT_USE("DONT USE"),
    FAKE("FAKE"),
    GIVEN_NAME("GIVEN NAME")
;
    private final String code;
    BadNameValues(String code) {
        this.code = code;
    }
    public static Optional<BadNameValues> findByValue(String value) {
        return Arrays.stream(values()).filter(name -> value.equalsIgnoreCase(name.code)).findFirst();
    }

}
