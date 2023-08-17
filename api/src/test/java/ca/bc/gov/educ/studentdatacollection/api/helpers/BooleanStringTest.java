package ca.bc.gov.educ.studentdatacollection.api.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BooleanStringTest {

  @Test
  void testEqual_whenGivenMatchingValues_BooleanTrueIsReturned() {
    assertThat(BooleanString.areEqual("true", Boolean.TRUE)).isTrue();
    assertThat(BooleanString.areEqual("true", "true")).isTrue();
    assertThat(BooleanString.areEqual("false", Boolean.FALSE)).isTrue();
    assertThat(BooleanString.areEqual("false", "false")).isTrue();
  }

  @Test
  void testEqual_whenGivenMismatchedValues_BooleanFalseIsReturned() {
    assertThat(BooleanString.areEqual("true", Boolean.FALSE)).isFalse();
    assertThat(BooleanString.areEqual("true", "false")).isFalse();
    assertThat(BooleanString.areEqual("false", Boolean.TRUE)).isFalse();
    assertThat(BooleanString.areEqual("false", "true")).isFalse();
  }

  @Test
  void testEqual_whenGivenANullishDefaultOfTrue_NullValuesAreTruthy() {
    assertThat(BooleanString.areEqual(null, Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.areEqual(null, "true", true)).isTrue();
    assertThat(BooleanString.areEqual("", Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.areEqual("", "true", true)).isTrue();
  }

  @Test
  void testEqual_whenGivenANullishDefaultOfFalse_NullValuesAreFalsy() {
    assertThat(BooleanString.areEqual(null, Boolean.TRUE, false)).isFalse();
    assertThat(BooleanString.areEqual(null, "true", false)).isFalse();
    assertThat(BooleanString.areEqual("", Boolean.FALSE, false)).isTrue();
    assertThat(BooleanString.areEqual("", "false", false)).isTrue();
  }
}
