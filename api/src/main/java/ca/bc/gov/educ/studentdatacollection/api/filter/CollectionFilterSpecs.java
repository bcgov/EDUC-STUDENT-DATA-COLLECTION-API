package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class CollectionFilterSpecs extends BaseFilterSpecs<CollectionEntity> {

  public CollectionFilterSpecs(FilterSpecifications<CollectionEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<CollectionEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<CollectionEntity, Integer> integerFilterSpecifications, FilterSpecifications<CollectionEntity, String> stringFilterSpecifications, FilterSpecifications<CollectionEntity, Long> longFilterSpecifications, FilterSpecifications<CollectionEntity, UUID> uuidFilterSpecifications, FilterSpecifications<CollectionEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }
}
