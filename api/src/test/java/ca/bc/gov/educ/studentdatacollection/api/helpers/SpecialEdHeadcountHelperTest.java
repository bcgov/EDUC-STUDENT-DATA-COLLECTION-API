package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import static org.wildfly.common.Assert.assertFalse;
import static org.wildfly.common.Assert.assertTrue;

class SpecialEdHeadcountHelperTest {

  @InjectMocks
  private SpecialEdHeadcountHelper specialEdHeadcountHelper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @ParameterizedTest
  @CsvSource({
          "INDEPEND",
          "INDP_FNS"
  })
  void testSetGradeCodes_IndependentSchool(String schoolCategoryCode) {
    School school = new School();
    school.setSchoolCategoryCode(schoolCategoryCode);
    specialEdHeadcountHelper.setGradeCodes(java.util.Optional.of(school));
    assertTrue(specialEdHeadcountHelper.gradeCodes.contains(SchoolGradeCodes.KINDHALF.getCode()));
  }

  @Test
  void testSetGradeCodes_NonIndependentSchool() {
    School school = new School();
    school.setSchoolCategoryCode(SchoolCategoryCodes.PUBLIC.getCode());
    specialEdHeadcountHelper.setGradeCodes(java.util.Optional.of(school));
    assertFalse(specialEdHeadcountHelper.gradeCodes.contains(SchoolGradeCodes.KINDHALF.getCode()));
  }

  @Test
  void testSetGradeCodes_NoSchool() {
    specialEdHeadcountHelper.setGradeCodes(java.util.Optional.empty());
    assertFalse(specialEdHeadcountHelper.gradeCodes.contains(SchoolGradeCodes.KINDHALF.getCode()));
  }
}
