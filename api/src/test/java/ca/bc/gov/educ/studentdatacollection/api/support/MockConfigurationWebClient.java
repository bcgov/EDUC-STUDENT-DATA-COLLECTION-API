package ca.bc.gov.educ.studentdatacollection.api.support;

import ca.bc.gov.educ.studentdatacollection.api.messaging.MessagePublisher;
import ca.bc.gov.educ.studentdatacollection.api.messaging.MessageSubscriber;
import ca.bc.gov.educ.studentdatacollection.api.messaging.NatsConnection;
import io.nats.client.Connection;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * The type Mock configuration.
 */
@Profile("testWebclient")
@Configuration
public class MockConfigurationWebClient {

  @Bean
  @Primary
  public MessagePublisher messagePublisher() {
    return Mockito.mock(MessagePublisher.class);
  }

  @Bean
  @Primary
  public MessageSubscriber messageSubscriber() {
    return Mockito.mock(MessageSubscriber.class);
  }

  @Bean
  @Primary
  public WebClient webClient() {
    return Mockito.mock(WebClient.class);
  }


  @Bean
  @Primary
  public Connection connection() {
    return Mockito.mock(Connection.class);
  }

  @Bean
  @Primary
  public NatsConnection natsConnection() {
    return Mockito.mock(NatsConnection.class);
  }

}
