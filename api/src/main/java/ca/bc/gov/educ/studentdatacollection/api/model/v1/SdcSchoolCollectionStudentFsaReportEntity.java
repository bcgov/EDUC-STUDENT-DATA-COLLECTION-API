package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionStudentFsaReportEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
            @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
    @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    UUID sdcSchoolCollectionStudentID;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false, targetEntity = SdcSchoolCollectionLightEntity.class)
    @JoinColumn(name = "SDC_SCHOOL_COLLECTION_ID", referencedColumnName = "SDC_SCHOOL_COLLECTION_ID", updatable = false)
    private SdcSchoolCollectionLightEntity  sdcSchoolCollection;

    @Column(name = "LEGAL_FIRST_NAME")
    private String legalFirstName;

    @Column(name = "LEGAL_LAST_NAME")
    private String legalLastName;

    @Column(name = "ENROLLED_GRADE_CODE")
    private String enrolledGradeCode;

    @Column(name = "ASSIGNED_PEN")
    private String assignedPen;

    @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE")
    private String sdcSchoolCollectionStudentStatusCode;
}
