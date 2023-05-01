package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SdcSchoolCollectionStudentFilterSpecs extends BaseFilterSpecs<SdcSchoolCollectionStudentEntity> {

  public SdcSchoolCollectionStudentFilterSpecs(FilterSpecifications<SdcSchoolCollectionStudentEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentEntity, Integer> integerFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentEntity, String> stringFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentEntity, Long> longFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentEntity, UUID> uuidFilterSpecifications,FilterSpecifications<SdcSchoolCollectionStudentEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
