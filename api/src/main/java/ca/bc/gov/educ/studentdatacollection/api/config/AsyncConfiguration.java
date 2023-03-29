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

  /**
   * Controller task executor executor.
   *
   * @return the executor
   */
  @Bean(name = "taskExecutor")
  public Executor controllerTaskExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().withNameFormat("async-executor-%d").get())
      .setCorePoolSize(4).setMaximumPoolSize(4).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }

  @Bean(name = "publisherExecutor")
  public Executor publisherExecutor() {
    return new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new com.google.common.util.concurrent.ThreadFactoryBuilder().setNameFormat("message-publisher-%d").build())
      .setCorePoolSize(2).setMaximumPoolSize(2).setKeepAliveTime(Duration.ofSeconds(60)).build();
  }
}
