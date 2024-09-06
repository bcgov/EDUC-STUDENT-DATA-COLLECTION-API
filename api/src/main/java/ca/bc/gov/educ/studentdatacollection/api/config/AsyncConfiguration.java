package ca.bc.gov.educ.studentdatacollection.api.config;

import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.util.ThreadFactoryBuilder;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableAsync;

import java.time.Duration;
import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Profile("!test")
public class AsyncConfiguration {
  /**
   * Thread pool task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "subscriberExecutor")
  @Autowired
  public Executor threadPoolTaskExecutor(final ApplicationProperties applicationProperties) {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("message-subscriber-%d").get())
      .setCorePoolSize(applicationProperties.getMinSubscriberThreads()).setMaximumPoolSize(applicationProperties.getMaxSubscriberThreads()).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "sagaRetryTaskExecutor")
  public Executor sagaRetryTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-saga-retry-executor-%d").get())
            .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "processLoadedStudentsTaskExecutor")
  public Executor processLoadedStudentsTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-loaded-stud-executor-%d").get())
      .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "processUncompletedSagasTaskExecutor")
  public Executor processUncompletedSagasTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-uncompleted-saga-executor-%d").get())
            .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "findSchoolCollectionsForSubmissionTaskExecutor")
  public Executor findSchoolCollectionsForSubmissionTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-school-collections-executor-%d").get())
            .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "findAllUnsubmittedIndependentSchoolsTaskExecutor")
  public Executor findAllUnsubmittedIndependentSchoolsTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-unsubmitted-indies-executor-%d").get())
            .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "updateStudentDemogDownstreamTaskExecutor")
  public Executor updateStudentDemogDownstreamTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
            .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-update-demog-executor-%d").get())
            .setCorePoolSize(5).setMaximumPoolSize(10).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "publisherExecutor")
  public Executor publisherExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new com.google.common.util.concurrent.ThreadFactoryBuilder().setNameFormat("message-publisher-%d").build())
      .setCorePoolSize(10).setMaximumPoolSize(20).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }
}
