package ca.bc.gov.educ.studentdatacollection.api.helpers;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BooleanStringTest {

  @Test
  void testAreEqual_whenGivenInvalidStringValue_ThrowsIllegalArgumentException() {
    Exception e = assertThrows(IllegalArgumentException.class, () ->
      BooleanString.areEqual("true", "truth")
    );

    assertThat(e.getMessage().equals("Boolean string must be a value of \"true\" or \"false\"")).isTrue();
  }

  @Test
  void testAreEqual_whenGivenMatchingValues_BooleanTrueIsReturned() {
    assertThat(BooleanString.areEqual("true", Boolean.TRUE)).isTrue();
    assertThat(BooleanString.areEqual("true", "true")).isTrue();
    assertThat(BooleanString.areEqual("false", Boolean.FALSE)).isTrue();
    assertThat(BooleanString.areEqual("false", "false")).isTrue();
  }

  @Test
  void testAreEqual_whenGivenMismatchedValues_BooleanFalseIsReturned() {
    assertThat(BooleanString.areEqual("true", Boolean.FALSE)).isFalse();
    assertThat(BooleanString.areEqual("true", "false")).isFalse();
    assertThat(BooleanString.areEqual("false", Boolean.TRUE)).isFalse();
    assertThat(BooleanString.areEqual("false", "true")).isFalse();
  }

  @Test
  void testAreEqual_whenGivenANullishDefaultOfTrue_NullValuesAreTruthy() {
    assertThat(BooleanString.areEqual(null, Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.areEqual(null, "true", true)).isTrue();
    assertThat(BooleanString.areEqual("", Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.areEqual("", "true", true)).isTrue();
  }

  @Test
  void testAreEqual_whenGivenANullishDefaultOfFalse_NullValuesAreFalsy() {
    assertThat(BooleanString.areEqual(null, Boolean.TRUE, false)).isFalse();
    assertThat(BooleanString.areEqual(null, "true", false)).isFalse();
    assertThat(BooleanString.areEqual("", Boolean.FALSE, false)).isTrue();
    assertThat(BooleanString.areEqual("", "false", false)).isTrue();
  }
}
