package ca.bc.gov.educ.studentdatacollection.api.filter;

import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * this is the generic class to support all kind of filter specifications for different entities
 *
 * @param <R> the entity type.
 * @author Om
 */
@RequiredArgsConstructor
public abstract class BaseFilterSpecs<R> {

  private final FilterSpecifications<R, ChronoLocalDate> dateFilterSpecifications;
  private final FilterSpecifications<R, ChronoLocalDateTime<?>> dateTimeFilterSpecifications;
  private final FilterSpecifications<R, Integer> integerFilterSpecifications;
  private final FilterSpecifications<R, String> stringFilterSpecifications;
  private final FilterSpecifications<R, Long> longFilterSpecifications;
  private final FilterSpecifications<R, UUID> uuidFilterSpecifications;
  private final FilterSpecifications<R, Boolean> booleanFilterSpecifications;
  private final Converters converters;

  /**
   * Gets date type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date type specification
   */
  public Specification<R> getDateTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDate.class), dateFilterSpecifications, null);
  }

  /**
   * Gets date time type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the date time type specification
   */
  public Specification<R> getDateTimeTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(ChronoLocalDateTime.class), dateTimeFilterSpecifications, null);
  }

  /**
   * Gets integer type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the integer type specification
   */
  public Specification<R> getIntegerTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Integer.class), integerFilterSpecifications, null);
  }

  /**
   * Gets long type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the long type specification
   */
  public Specification<R> getLongTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Long.class), longFilterSpecifications, null);
  }

  /**
   * Gets string type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the string type specification
   */
  public Specification<R> getStringTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation, String districtCollectionID) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(String.class), stringFilterSpecifications, districtCollectionID);
  }

  /**
   * Gets boolean type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the string type specification
   */
  public Specification<R> getBooleanTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(Boolean.class), booleanFilterSpecifications, null);
  }

  /**
   * Gets uuid type specification.
   *
   * @param fieldName       the field name
   * @param filterValue     the filter value
   * @param filterOperation the filter operation
   * @return the uuid type specification
   */
  public Specification<R> getUUIDTypeSpecification(String fieldName, String filterValue, FilterOperation filterOperation) {
    return getSpecification(fieldName, filterValue, filterOperation, converters.getFunction(UUID.class), uuidFilterSpecifications, null);
  }

  private <T extends Comparable<T>> Specification<R> getSpecification(String fieldName,
                                                                      String filterValue,
                                                                      FilterOperation filterOperation,
                                                                      Function<String, T> converter,
                                                                      FilterSpecifications<R, T> specifications,
                                                                      String districtCollectionID) {
    FilterCriteria<T> criteria = new FilterCriteria<>(fieldName, filterValue, filterOperation, converter, districtCollectionID);
    return specifications.getSpecification(criteria.getOperation()).apply(criteria);
  }
}
