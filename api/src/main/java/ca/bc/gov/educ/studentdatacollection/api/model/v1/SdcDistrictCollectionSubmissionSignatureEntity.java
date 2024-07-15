package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_DISTRICT_COLLECTION_SUBMISSION_SIGNATURE")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcDistrictCollectionSubmissionSignatureEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
            @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
    @Column(name = "SDC_DISTRICT_SUBMISSION_SIGNATURE_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    private UUID sdcDistrictSubmissionSignatureID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = SdcDistrictCollectionEntity.class)
    @JoinColumn(name = "SDC_DISTRICT_COLLECTION_ID", referencedColumnName = "SDC_DISTRICT_COLLECTION_ID", updatable = false)
    SdcDistrictCollectionEntity sdcDistrictCollection;

    @Column(name = "DISTRICT_SIGNATORY_USER_ID", length = 32)
    private String districtSignatoryUserID;

    @Column(name = "DISTRICT_SIGNATORY_ROLE", length = 30)
    private String districtSignatoryRole;

    @PastOrPresent
    @Column(name = "SIGNATURE_DATE")
    private LocalDateTime signatureDate;

    @Column(name = "CREATE_USER", updatable = false , length = 32)
    private String createUser;

    @PastOrPresent
    @Column(name = "CREATE_DATE", updatable = false)
    private LocalDateTime createDate;

    @Column(name = "UPDATE_USER", length = 32)
    private String updateUser;

    @PastOrPresent
    @Column(name = "UPDATE_DATE")
    private LocalDateTime updateDate;
}
