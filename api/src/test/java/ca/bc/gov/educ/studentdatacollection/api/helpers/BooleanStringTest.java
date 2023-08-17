package ca.bc.gov.educ.studentdatacollection.api.helpers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BooleanStringTest {

  @Test
  void testEqual_whenGivenMatchingValues_BooleanTrueIsReturned() {
    assertThat(BooleanString.equal("true", Boolean.TRUE)).isTrue();
    assertThat(BooleanString.equal("true", "true")).isTrue();
    assertThat(BooleanString.equal("false", Boolean.FALSE)).isTrue();
    assertThat(BooleanString.equal("false", "false")).isTrue();
  }

  @Test
  void testEqual_whenGivenMismatchedValues_BooleanFalseIsReturned() {
    assertThat(BooleanString.equal("true", Boolean.FALSE)).isFalse();
    assertThat(BooleanString.equal("true", "false")).isFalse();
    assertThat(BooleanString.equal("false", Boolean.TRUE)).isFalse();
    assertThat(BooleanString.equal("false", "true")).isFalse();
  }

  @Test
  void testEqual_whenGivenANullishDefaultOfTrue_NullValuesAreTruthy() {
    assertThat(BooleanString.equal(null, Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.equal(null, "true", true)).isTrue();
    assertThat(BooleanString.equal("", Boolean.TRUE, true)).isTrue();
    assertThat(BooleanString.equal("", "true", true)).isTrue();
  }

  @Test
  void testEqual_whenGivenANullishDefaultOfFalse_NullValuesAreFalsy() {
    assertThat(BooleanString.equal(null, Boolean.TRUE, false)).isFalse();
    assertThat(BooleanString.equal(null, "true", false)).isFalse();
    assertThat(BooleanString.equal("", Boolean.FALSE, false)).isTrue();
    assertThat(BooleanString.equal("", "false", false)).isTrue();
  }
}
