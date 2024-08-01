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
@Table(name = "SDC_SAGA")
@DynamicUpdate
public class SdcSagaEntity {

  @Id
  @GeneratedValue(generator = "UUID")
  @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
      @org.hibernate.annotations.Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
  @Column(name = "SAGA_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
  UUID sagaId;

  @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", columnDefinition = "BINARY(16)")
  UUID sdcSchoolCollectionStudentID;

  @Column(name = "SDC_SCHOOL_COLLECTION_ID", columnDefinition = "BINARY(16)")
  UUID sdcSchoolCollectionID;

  @Column(name = "COLLECTION_ID", columnDefinition = "BINARY(16)")
  UUID collectionID;

  @NotNull(message = "saga name cannot be null")
  @Column(name = "SAGA_NAME")
  String sagaName;

  @NotNull(message = "saga state cannot be null")
  @Column(name = "SAGA_STATE")
  String sagaState;

  @NotNull(message = "payload cannot be null")
  @Column(name = "PAYLOAD",  length = 10485760)
  private String payload;

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

}
