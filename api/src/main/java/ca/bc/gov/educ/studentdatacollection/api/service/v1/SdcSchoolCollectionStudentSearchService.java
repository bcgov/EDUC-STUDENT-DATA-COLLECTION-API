package ca.bc.gov.educ.studentdatacollection.api.service.v1;


import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.filter.SdcSchoolCollectionStudentFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentLightRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentPaginationRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Condition;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentSearchService {
  private final SdcSchoolCollectionStudentFilterSpecs sdcSchoolCollectionStudentFilterSpecs;

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private final SdcSchoolCollectionStudentPaginationRepository sdcSchoolCollectionStudentPaginationRepository;

  private final SdcSchoolCollectionStudentLightRepository sdcSchoolCollectionStudentLightRepository;

  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  public Specification<SdcSchoolCollectionStudentPaginationEntity> getSpecifications(Specification<SdcSchoolCollectionStudentPaginationEntity> schoolSpecs, int i, Search search) {
    if (i == 0) {
      schoolSpecs = getSchoolCollectionStudentEntitySpecification(search.getSearchCriteriaList());
    } else {
      if (search.getCondition() == Condition.AND) {
        schoolSpecs = schoolSpecs.and(getSchoolCollectionStudentEntitySpecification(search.getSearchCriteriaList()));
      } else {
        schoolSpecs = schoolSpecs.or(getSchoolCollectionStudentEntitySpecification(search.getSearchCriteriaList()));
      }
    }
    return schoolSpecs;
  }

  private Specification<SdcSchoolCollectionStudentPaginationEntity> getSchoolCollectionStudentEntitySpecification(List<SearchCriteria> criteriaList) {
    Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs = null;
    if (!criteriaList.isEmpty()) {
      int i = 0;
      for (SearchCriteria criteria : criteriaList) {
        if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
          var criteriaValue = criteria.getValue();
          if(StringUtils.isNotBlank(criteria.getValue()) && TransformUtil.isUppercaseField(SdcSchoolCollectionStudentEntity.class, criteria.getKey())) {
            criteriaValue = criteriaValue.toUpperCase();
          }
          Specification<SdcSchoolCollectionStudentPaginationEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteriaValue, criteria.getValueType());
          studentSpecs = getSpecificationPerGroup(studentSpecs, i, criteria, typeSpecification);
          i++;
        } else {
          throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
        }
      }
    }
    return studentSpecs;
  }

  private Specification<SdcSchoolCollectionStudentPaginationEntity> getSpecificationPerGroup(Specification<SdcSchoolCollectionStudentPaginationEntity> schoolEntitySpecification, int i, SearchCriteria criteria, Specification<SdcSchoolCollectionStudentPaginationEntity> typeSpecification) {
    if (i == 0) {
      schoolEntitySpecification = Specification.where(typeSpecification);
    } else {
      if (criteria.getCondition() == Condition.AND) {
        schoolEntitySpecification = schoolEntitySpecification.and(typeSpecification);
      } else {
        schoolEntitySpecification = schoolEntitySpecification.or(typeSpecification);
      }
    }
    return schoolEntitySpecification;
  }

  private Specification<SdcSchoolCollectionStudentPaginationEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
    Specification<SdcSchoolCollectionStudentPaginationEntity> schoolEntitySpecification = null;
    switch (valueType) {
      case STRING:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
        break;
      case DATE_TIME:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
        break;
      case LONG:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getLongTypeSpecification(key, value, filterOperation);
        break;
      case INTEGER:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
        break;
      case DATE:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getDateTypeSpecification(key, value, filterOperation);
        break;
      case UUID:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
        break;
      case BOOLEAN:
        schoolEntitySpecification = sdcSchoolCollectionStudentFilterSpecs.getBooleanTypeSpecification(key, value, filterOperation);
        break;
      default:
        break;
    }
    return schoolEntitySpecification;
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<SdcSchoolCollectionStudentPaginationEntity>> findAll(Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    log.info("Starting findAll");
    log.trace("In find all query: {}", studentSpecs);
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        log.trace("Running paginated query: {}", studentSpecs);
        var results = this.sdcSchoolCollectionStudentPaginationRepository.findAll(studentSpecs, paging);
        log.trace("Paginated query returned with results: {}", results);
        log.info("Finish find all");
        return results;
      } catch (final Throwable ex) {
        log.error("Failure querying for paginated SDC school students: {}", ex.getMessage());
        throw new CompletionException(ex);
      }
    }, paginatedQueryExecutor);

  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public List<SdcSchoolCollectionStudentLightEntity> findAllSync(UUID collectionID) {
  log.info("Starting findAll CompletableFuture<List<SdcSchoolCollectionStudentLightEntity>>");
    try {
      var results = this.sdcSchoolCollectionStudentLightRepository.findAllBySdcSchoolCollectionID(collectionID);
      log.info("Finish find all CompletableFuture<List<SdcSchoolCollectionStudentLightEntity>>");
      log.info(String.valueOf((long) results.size()));
      return results;
    } catch (final Throwable ex) {
      log.error("Failure querying for paginated SDC school students: {}", ex.getMessage());
      throw new CompletionException(ex);
    }
  }

  public Specification<SdcSchoolCollectionStudentPaginationEntity> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
    Specification<SdcSchoolCollectionStudentPaginationEntity> schoolSpecs = null;
    try {
      RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
      if (StringUtils.isNotBlank(searchCriteriaListJson)) {
        List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
        });
        int i = 0;
        for (var search : searches) {
          schoolSpecs = getSpecifications(schoolSpecs, i, search);
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new StudentDataCollectionAPIRuntimeException(e.getMessage());
    }
    return schoolSpecs;
  }
}
