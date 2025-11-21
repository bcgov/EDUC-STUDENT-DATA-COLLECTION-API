package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.SdcSchoolCollectionStudentHistoryFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentHistoryPaginationRepository;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentHistorySearchService extends BaseSearchService {
    @Getter
    private final SdcSchoolCollectionStudentHistoryFilterSpecs sdcSchoolCollectionStudentHistoryFilterSpecs;

    private final SdcSchoolCollectionStudentHistoryPaginationRepository sdcSchoolCollectionStudentHistoryPaginationRepository;

    private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
            .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

    @Transactional(propagation = Propagation.SUPPORTS)
    public CompletableFuture<Page<SdcSchoolCollectionStudentHistoryPaginationEntity>> findAll(Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> studentSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
        log.trace("In find all query: {}", studentSpecs);
        return CompletableFuture.supplyAsync(() -> {
            Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
            try {
                log.trace("Running paginated query: {}", studentSpecs);
                var results = this.sdcSchoolCollectionStudentHistoryPaginationRepository.findAll(studentSpecs, paging);
                log.trace("Paginated query returned with results: {}", results);
                return results;
            } catch (final Throwable ex) {
                log.error("Failure querying for paginated SDC school students: {}", ex.getMessage());
                throw new CompletionException(ex);
            }
        }, paginatedQueryExecutor);
    }

    public Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> setSpecificationAndSortCriteria(String sortCriteriaJson, String searchCriteriaListJson, ObjectMapper objectMapper, List<Sort.Order> sorts) {
        Specification<SdcSchoolCollectionStudentHistoryPaginationEntity> schoolSpecs = null;
        try {
            RequestUtil.getSortCriteria(sortCriteriaJson, objectMapper, sorts);
            if (StringUtils.isNotBlank(searchCriteriaListJson)) {
                List<Search> searches = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
                });
                int i = 0;
                for (var search : searches) {
                    schoolSpecs = getSpecifications(schoolSpecs, i, search, getSdcSchoolCollectionStudentHistoryFilterSpecs(), searches);
                    i++;
                }
            }
        } catch (JsonProcessingException e) {
            throw new StudentDataCollectionAPIRuntimeException(e.getMessage());
        }
        return schoolSpecs;
    }
}
