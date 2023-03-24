package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The type Saga event.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "COLLECTION_EVENT")
@DynamicUpdate
public class CollectionEvent {

  /**
   * The Create user.
   */
  @Column(name = "CREATE_USER", updatable = false)
  String createUser;
  /**
   * The Create date.
   */
  @Column(name = "CREATE_DATE", updatable = false)
  @PastOrPresent
  LocalDateTime createDate;
  /**
   * The Update user.
   */
  @Column(name = "UPDATE_USER")
  String updateUser;
  /**
   * The Update date.
   */
  @Column(name = "UPDATE_DATE")
  @PastOrPresent
  LocalDateTime updateDate;
  /**
   * The Event id.
   */
  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
    @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "EVENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  private UUID eventId;
  /**
   * The Event payload bytes.
   */
  @NotNull(message = "eventPayload cannot be null")
  @Lob
  @Column(name = "EVENT_PAYLOAD")
  private byte[] eventPayloadBytes;
  /**
   * The Event status.
   */
  @NotNull(message = "eventStatus cannot be null")
  @Column(name = "EVENT_STATUS")
  private String eventStatus;
  /**
   * The Event type.
   */
  @NotNull(message = "eventType cannot be null")
  @Column(name = "EVENT_TYPE")
  private String eventType;
  /**
   * The Saga id.
   */
  @Column(name = "SAGA_ID", updatable = false)
  private UUID sagaId;
  /**
   * The Event outcome.
   */
  @NotNull(message = "eventOutcome cannot be null.")
  @Column(name = "EVENT_OUTCOME")
  private String eventOutcome;
  /**
   * The Reply channel.
   */
  @Column(name = "REPLY_CHANNEL")
  private String replyChannel;

  /**
   * Gets event payload.
   *
   * @return the event payload
   */
  public String getEventPayload() {
    return new String(this.getEventPayloadBytes(), StandardCharsets.UTF_8);
  }

  /**
   * Sets event payload.
   *
   * @param eventPayload the event payload
   */
  public void setEventPayload(final String eventPayload) {
    this.setEventPayloadBytes(eventPayload.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * The type Services event builder.
   */
  public static class CollectionEventBuilder {
    /**
     * The Event payload bytes.
     */
    byte[] eventPayloadBytes;

    public CollectionEvent.CollectionEventBuilder eventPayload(final String eventPayload) {
      this.eventPayloadBytes = eventPayload.getBytes(StandardCharsets.UTF_8);
      return this;
    }
  }
}
