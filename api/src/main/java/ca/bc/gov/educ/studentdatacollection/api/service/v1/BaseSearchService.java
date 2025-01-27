package ca.bc.gov.educ.studentdatacollection.api.service.v1;


import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.filter.BaseFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class BaseSearchService {

  public <T> Specification<T> getSpecifications(Specification<T> specs, int i, Search search, BaseFilterSpecs<T> filterSpecs, List<Search> searches) {
    if (i == 0) {
      specs = getEntitySpecification(search.getSearchCriteriaList(), filterSpecs, searches);
    } else {
      if (search.getCondition() == Condition.AND) {
        specs = specs.and(getEntitySpecification(search.getSearchCriteriaList(), filterSpecs, searches));
      } else {
        specs = specs.or(getEntitySpecification(search.getSearchCriteriaList(), filterSpecs, searches));
      }
    }
    return specs;
  }

  private <T> Specification<T> getEntitySpecification(List<SearchCriteria> criteriaList, BaseFilterSpecs<T> filterSpecs, List<Search> searches) {
    Specification<T> specs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          var criteriaValue = criteria.getValue();
          if(StringUtils.isNotBlank(criteria.getValue()) && TransformUtil.isUppercaseField(SdcSchoolCollectionEntity.class, criteria.getKey())) {
            criteriaValue = criteriaValue.toUpperCase();
          }
          Specification<T> typeSpecification = null;
          if(criteria.getOperation().equals(FilterOperation.NONE_IN_DISTRICT)) {
            Optional<Search> sdcDistrictCollectionIDValOptional = searches.stream().filter(v -> v.getSearchCriteriaList().stream().anyMatch(j -> j.getKey().equalsIgnoreCase("sdcSchoolCollection.sdcDistrictCollectionID"))).findAny();
            var sdcDistrictCollectionIDFilter = sdcDistrictCollectionIDValOptional.orElseThrow(() ->
                    new EntityNotFoundException(Search.class, "districtCollectionID"));
            var districtCollectionIDValue = sdcDistrictCollectionIDFilter.getSearchCriteriaList().stream().filter(j -> j.getKey().equalsIgnoreCase("sdcSchoolCollection.sdcDistrictCollectionID")).findFirst();
            var sdcDistrictCollectionID = districtCollectionIDValue.orElseThrow(() ->
                    new EntityNotFoundException(Search.class, "districtCollectionID"));
            typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType(), filterSpecs, sdcDistrictCollectionID.getValue());
          } else {
            typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType(), filterSpecs, null);
          }
          specs = getSpecificationPerGroup(specs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return specs;
  }

  private <T> Specification<T> getSpecificationPerGroup(Specification<T> entitySpecification, int i, SearchCriteria criteria, Specification<T> typeSpecification) {
    if (i == 0) {
      entitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        entitySpecification = entitySpecification.and(typeSpecification);
      } else {
        entitySpecification = entitySpecification.or(typeSpecification);
      }
    }
    return entitySpecification;
  }

  private <T> Specification<T> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType, BaseFilterSpecs<T> filterSpecs, String districtCollectionID) {
    Specification<T> entitySpecification = null;
    switch (valueType) {
      case STRING ->
              entitySpecification = filterSpecs.getStringTypeSpecification(key, value, filterOperation, districtCollectionID);
      case DATE_TIME ->
              entitySpecification = filterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
      case LONG ->
              entitySpecification = filterSpecs.getLongTypeSpecification(key, value, filterOperation);
      case INTEGER ->
              entitySpecification = filterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
      case DATE ->
              entitySpecification = filterSpecs.getDateTypeSpecification(key, value, filterOperation);
      case UUID ->
              entitySpecification = filterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
      case BOOLEAN ->
              entitySpecification = filterSpecs.getBooleanTypeSpecification(key, value, filterOperation);
    }
    return entitySpecification;
  }
}
