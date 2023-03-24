package ca.bc.gov.educ.studentdatacollection.api.config;

import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.rules.Rule;
import ca.bc.gov.educ.studentdatacollection.api.rules.impl.BirthDateRule;
import ca.bc.gov.educ.studentdatacollection.api.rules.impl.GenderRule;
import ca.bc.gov.educ.studentdatacollection.api.rules.impl.GradeCodeRule;
import ca.bc.gov.educ.studentdatacollection.api.rules.impl.SurnameRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
public class RulesConfig {

  @Bean
  @Order(1)
  public Rule surnameRule() {
    return new SurnameRule();
  }

  @Bean
  @Order(2)
  @Autowired
  public Rule genderRule(final RestUtils restUtils) {
    return new GenderRule();
  }

  @Bean
  @Order(3)
  public Rule birthDateRule() {
    return new BirthDateRule();
  }

  @Bean
  @Order(4)
  @Autowired
  public Rule gradeCodeRule(final RestUtils restUtils) {
    return new GradeCodeRule();
  }
}
