package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
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
 * The type Saga.
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
@Entity
@Table(name = "STUDENT_DATA_COLLECTION_SAGA")
@DynamicUpdate
public class Saga {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SAGA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaId;

  @Column(name = "STUDENT_DATA_COLLECTION_STUDENT_ID", columnDefinition = "BINARY(16)")
  UUID sdcSchoolStudentID;

  @Column(name = "STUDENT_DATA_COLLECTION_SCHOOL_ID", columnDefinition = "BINARY(16)")
  UUID sdcSchoolBatchID;

  @NotNull(message = "saga name cannot be null")
  @Column(name = "SAGA_NAME")
  String sagaName;

  @NotNull(message = "saga state cannot be null")
  @Column(name = "SAGA_STATE")
  String sagaState;

  @NotNull(message = "payload cannot be null")
  @Lob
  @Column(name = "PAYLOAD")
  byte[] payloadBytes;

  @NotNull(message = "status cannot be null")
  @Column(name = "STATUS")
  String status;

  @NotNull(message = "create user cannot be null")
  @Column(name = "CREATE_USER", updatable = false)
  @Size(max = 32)
  String createUser;

  @NotNull(message = "update user cannot be null")
  @Column(name = "UPDATE_USER")
  @Size(max = 32)
  String updateUser;

  @PastOrPresent
  @Column(name = "CREATE_DATE", updatable = false)
  LocalDateTime createDate;

  @PastOrPresent
  @Column(name = "UPDATE_DATE")
  LocalDateTime updateDate;

  @Column(name = "RETRY_COUNT")
  private Integer retryCount;

  public String getPayload() {
    return new String(this.getPayloadBytes(), StandardCharsets.UTF_8);
  }

  public void setPayload(final String payload) {
    this.setPayloadBytes(payload.getBytes(StandardCharsets.UTF_8));
  }

  public static class SagaBuilder {
    byte[] payloadBytes;

    public SagaBuilder payload(final String payload) {
      this.payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
      return this;
    }
  }

}
