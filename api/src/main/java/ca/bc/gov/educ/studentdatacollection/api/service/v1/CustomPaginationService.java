package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentPaginationRepositoryLight;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
public class CustomPaginationService {

    private final SdcSchoolCollectionStudentPaginationRepositoryLight customSdcSchoolCollectionStudentPaginationRepositoryLight;

    private final Executor paginatedQueryExecutor = new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("async-pagination-query-executor-%d").build())
            .setCorePoolSize(2).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();

    @Transactional(propagation = Propagation.SUPPORTS)
    public CompletableFuture<Slice<SdcSchoolCollectionStudentPaginationEntity>> findAllWithoutCount(Specification<SdcSchoolCollectionStudentPaginationEntity> studentSpecs, Pageable pageable) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.trace("Running paginated query: {}", studentSpecs);
                Slice<SdcSchoolCollectionStudentPaginationEntity> results = this.customSdcSchoolCollectionStudentPaginationRepositoryLight.findAllWithoutCount(studentSpecs, pageable);
                log.trace("Paginated query returned with results: {}", results);
                return results;
            } catch (final Throwable ex) {
                log.error("Failure querying for paginated SDC school students without count: {}", ex.getMessage());
                throw new CompletionException(ex);
            }
        }, paginatedQueryExecutor);
    }
}
