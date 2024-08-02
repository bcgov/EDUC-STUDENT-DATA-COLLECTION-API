package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SdcDistrictCollectionFilterSpecs extends BaseFilterSpecs<SdcDistrictCollectionEntity> {

  public SdcDistrictCollectionFilterSpecs(FilterSpecifications<SdcDistrictCollectionEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, Integer> integerFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, String> stringFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, Long> longFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, UUID> uuidFilterSpecifications, FilterSpecifications<SdcDistrictCollectionEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
