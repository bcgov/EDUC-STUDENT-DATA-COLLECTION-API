package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.SdcSchoolCollectionStudentFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentLightRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentPaginationRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Search;
import ca.bc.gov.educ.studentdatacollection.api.util.RequestUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.*;
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
public class SdcSchoolCollectionStudentSearchService extends BaseSearchService {
  @Getter
  private final SdcSchoolCollectionStudentFilterSpecs sdcSchoolCollectionStudentFilterSpecs;

  private final SdcSchoolCollectionStudentPaginationRepository sdcSchoolCollectionStudentPaginationRepository;

  private final SdcSchoolCollectionStudentLightRepository sdcSchoolCollectionStudentLightRepository;

  private final CustomPaginationService customPaginationService;

  private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
    .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
    .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<SdcSchoolCollectionStudentPaginationEntity>> findAll(Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    log.trace("In find all query: {}", studentSpecs);
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        log.trace("Running paginated query: {}", studentSpecs);
        var results = this.sdcSchoolCollectionStudentPaginationRepository.findAll(studentSpecs, paging);
        log.trace("Paginated query returned with results: {}", results);
        return results;
      } catch (final Throwable ex) {
        log.error("Failure querying for paginated SDC school students: {}", ex.getMessage());
        throw new CompletionException(ex);
      }
    }, paginatedQueryExecutor);

  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Slice<SdcSchoolCollectionStudentPaginationEntity>> findAllSlice(Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    log.trace("In find all slice query: {}", studentSpecs);
    return CompletableFuture.supplyAsync(() -> {
      Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
      try {
        log.trace("Running paginated query without count: {}", studentSpecs);
        var results = this.customPaginationService.findAllWithoutCount(studentSpecs, paging).join();
        log.trace("Paginated query without count returned with results: {}", results);
        return results;
      } catch (final Throwable ex) {
        log.error("Failure querying for paginated SDC school students without count: {}", ex.getMessage());
        throw new CompletionException(ex);
      }
    }, paginatedQueryExecutor);
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public List<SdcSchoolCollectionStudentLightEntity> findAllStudentsLightBySchoolCollectionID(UUID collectionID) {
    try {
      return this.sdcSchoolCollectionStudentLightRepository.findAllBySdcSchoolCollectionID(collectionID);
    } catch (final Throwable ex) {
      log.error("Failure querying for all light SDC school students by School Collection ID: {}", ex.getMessage());
      throw new CompletionException(ex);
    }
  }

  @Transactional(propagation = Propagation.SUPPORTS)
  public List<SdcSchoolCollectionStudentLightEntity> findAllStudentsLightByDistrictCollectionId(UUID collectionID) {
    try {
      return this.sdcSchoolCollectionStudentLightRepository.findAllBySdcDistrictCollectionID(collectionID);
    } catch (final Throwable ex) {
      log.error("Failure querying for light SDC school students by District Collection ID: {}", ex.getMessage());
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
          schoolSpecs = getSpecifications(schoolSpecs, i, search, getSdcSchoolCollectionStudentFilterSpecs());
          i++;
        }
      }
    } catch (JsonProcessingException e) {
      throw new StudentDataCollectionAPIRuntimeException(e.getMessage());
    }
    return schoolSpecs;
  }
}
