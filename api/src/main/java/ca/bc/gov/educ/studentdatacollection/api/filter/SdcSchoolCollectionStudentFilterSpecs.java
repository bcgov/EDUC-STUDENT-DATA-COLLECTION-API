package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;
import java.util.function.Function;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentFilterSpecs {

  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, ChronoLocalDate> dateFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, Integer> integerFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, String> stringFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, Long> longFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, UUID> uuidFilterSpecifications;
  private final FilterSpecifications<SdcSchoolCollectionStudentEntity, Boolean> booleanFilterSpecifications;
  private final Converters converters;

  public Specification<SdcSchoolCollectionStudentEntity> getDateTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDate.class), dateFilterSpecifications);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getDateTimeTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDateTime.class), dateTimeFilterSpecifications);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getIntegerTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Integer.class), integerFilterSpecifications);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getLongTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Long.class), longFilterSpecifications);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getStringTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(String.class), stringFilterSpecifications);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getBooleanTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Boolean.class), booleanFilterSpecifications);
  }

  private <T extends Comparable<T>> Specification<SdcSchoolCollectionStudentEntity> getSpecification(String fieldName,
                                                                                   String filterValue,
                                                                                   FilterOperation filterOperation,
                                                                                   Function<String, T> converter,
                                                                                   FilterSpecifications<SdcSchoolCollectionStudentEntity, T> specifications) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }

  public Specification<SdcSchoolCollectionStudentEntity> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(UUID.class), uuidFilterSpecifications);
  }
}
