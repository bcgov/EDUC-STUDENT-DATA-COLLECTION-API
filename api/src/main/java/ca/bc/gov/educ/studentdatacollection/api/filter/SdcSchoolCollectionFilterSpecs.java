package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SdcSchoolCollectionFilterSpecs extends BaseFilterSpecs<SdcSchoolCollectionEntity> {

  public SdcSchoolCollectionFilterSpecs(FilterSpecifications<SdcSchoolCollectionEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, Integer> integerFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, String> stringFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, Long> longFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, UUID> uuidFilterSpecifications, FilterSpecifications<SdcSchoolCollectionEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
