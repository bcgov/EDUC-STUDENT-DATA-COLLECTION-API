package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import lombok.Data;
import lombok.val;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

public class BaseSdcSchoolStudentTest {

    private static Validator validator;

    @BeforeAll
    public static void setupValidatorInstance() {
        validator = Validation.buildDefaultValidatorFactory()
                .getValidator();
    }

    @Test
    void testBaseSdcSchoolStudent_GivenValidData_ShouldHaveNoValidationIssues() throws Exception {

        final File file = new File(
                Objects.requireNonNull(getClass().getClassLoader().getResource("valid-base-sdc-school-student-test-data.json")).getFile()
        );
        val student = new ObjectMapper().readValue(file, BaseSdcSchoolStudent.class);

        Set<ConstraintViolation<BaseSdcSchoolStudent>> violations = validator.validate(student);
        assertThat(violations).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidTestData")
    void testBaseSdcSchoolStudent_GivenInvalidData_ShouldHaveValidationIssue(BaseSdcSchoolStudent invalidStudent, String expectedErrorMessage, String expectedErrorField) {
        Set<ConstraintViolation<BaseSdcSchoolStudent>> violations = validator.validate(invalidStudent);
        assertThat(violations).hasSize(1);
        violations.forEach(action -> {
            assertThat(action.getMessage()).isEqualTo(expectedErrorMessage);
            assertThat(action.getPropertyPath()).hasToString(expectedErrorField);
        });
    }

    // Method to read the test data from the JSON file
    private static Stream<Arguments> invalidTestData() throws IOException {
        final File file = new File(
                Objects.requireNonNull(BaseSdcSchoolStudentTest.class.getClassLoader().getResource("invalid-base-sdc-school-students-test-data.json")).getFile()
        );
        ObjectMapper objectMapper = new ObjectMapper();
        List<InvalidTestCase> invalidTestCases = objectMapper.readValue(file, new TypeReference<>() {
        });
        return invalidTestCases.stream()
                .map(testCase -> arguments(testCase.getInvalidStudent(), testCase.getExpectedErrorMessage(), testCase.getExpectedErrorField()));
    }

    // Inner class to represent each test case from the JSON file
    @Data
    private static class InvalidTestCase {
        private BaseSdcSchoolStudent invalidStudent;
        private String expectedErrorMessage;
        private String expectedErrorField;
    }
}
