package ca.bc.gov.educ.studentdatacollection.api.model.v1;

import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import jakarta.persistence.*;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;
import org.hibernate.annotations.Parameter;
import org.hibernate.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@DynamicUpdate
@Entity
@Table(name = "SDC_SCHOOL_COLLECTION_STUDENT")
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcSchoolCollectionStudentLightWithValidationIssueCodesEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator", parameters = {
            @Parameter(name = "uuid_gen_strategy_class", value = "org.hibernate.id.uuid.CustomVersionOneStrategy")})
    @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_ID", unique = true, updatable = false, columnDefinition = "BINARY(16)")
    UUID sdcSchoolCollectionStudentID;

    @Column(name = "SDC_SCHOOL_COLLECTION_ID")
    private UUID sdcSchoolCollectionID;

    @Column(name = "LOCAL_ID")
    private String localID;

    @Column(name = "STUDENT_PEN")
    private String studentPen;

    @Column(name = "LEGAL_FIRST_NAME")
    @UpperCase
    private String legalFirstName;

    @Column(name = "LEGAL_MIDDLE_NAMES")
    @UpperCase
    private String legalMiddleNames;

    @Column(name = "LEGAL_LAST_NAME")
    @UpperCase
    private String legalLastName;

    @Column(name = "USUAL_FIRST_NAME")
    @UpperCase
    private String usualFirstName;

    @Column(name = "USUAL_MIDDLE_NAMES")
    @UpperCase
    private String usualMiddleNames;

    @Column(name = "USUAL_LAST_NAME")
    @UpperCase
    private String usualLastName;

    @Column(name = "DOB")
    private String dob;

    @Column(name = "GENDER_CODE", length = 1)
    private String gender;

    @Column(name = "SPECIAL_EDUCATION_CATEGORY_CODE")
    private String specialEducationCategoryCode;

    @Column(name = "SCHOOL_FUNDING_CODE")
    private String schoolFundingCode;

    @Column(name = "NATIVE_ANCESTRY_IND")
    private String nativeAncestryInd;

    @Column(name = "HOME_LANGUAGE_SPOKEN_CODE")
    private String homeLanguageSpokenCode;

    @Column(name = "OTHER_COURSES")
    private String otherCourses;

    @Column(name = "SUPPORT_BLOCKS")
    private String supportBlocks;

    @Column(name = "ENROLLED_GRADE_CODE")
    private String enrolledGradeCode;

    @Column(name = "ENROLLED_PROGRAM_CODES")
    private String enrolledProgramCodes;

    @Column(name = "CAREER_PROGRAM_CODE")
    private String careerProgramCode;

    @Column(name = "NUMBER_OF_COURSES")
    private String numberOfCourses;

    @Column(name= "NUMBER_OF_COURSES_DEC")
    private BigDecimal numberOfCoursesDec;

    @Column(name = "BAND_CODE")
    private String bandCode;

    @Column(name = "POSTAL_CODE")
    @UpperCase
    private String postalCode;

    @Column(name = "SDC_SCHOOL_COLLECTION_STUDENT_STATUS_CODE")
    private String sdcSchoolCollectionStudentStatusCode;

    @Column(name = "IS_ADULT")
    private Boolean isAdult;

    @Column(name = "IS_SCHOOL_AGED")
    private Boolean isSchoolAged;

    @Column(name = "FTE")
    private BigDecimal fte;

    @Column(name = "FTE_ZERO_REASON_CODE", length = 10)
    private String fteZeroReasonCode;

    @Column(name = "FRENCH_PROGRAM_NON_ELIG_REASON_CODE", length = 10)
    private String frenchProgramNonEligReasonCode;

    @Column(name = "ELL_NON_ELIG_REASON_CODE", length = 10)
    private String ellNonEligReasonCode;

    @Column(name = "INDIGENOUS_SUPPORT_PROGRAM_NON_ELIG_REASON_CODE", length = 10)
    private String indigenousSupportProgramNonEligReasonCode;

    @Column(name = "CAREER_PROGRAM_NON_ELIG_REASON_CODE", length = 10)
    private String careerProgramNonEligReasonCode;

    @Column(name = "SPECIAL_EDUCATION_NON_ELIG_REASON_CODE", length = 10)
    private String specialEducationNonEligReasonCode;

    @Column(name = "IS_GRADUATED")
    private Boolean isGraduated;

    @Column(name = "ASSIGNED_PEN")
    private String assignedPen;

    @Column(name = "ASSIGNED_STUDENT_ID")
    private UUID assignedStudentId;

    @Column(name = "YEARS_IN_ELL")
    private Integer yearsInEll;

    @Column(name = "ORIGINAL_DEMOG_HASH")
    private String originalDemogHash;

    @Column(name = "CURRENT_DEMOG_HASH")
    private String currentDemogHash;

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

    @ManyToOne
    @NotFound(action = NotFoundAction.IGNORE)
    @JoinColumn(name = "SDC_SCHOOL_COLLECTION_ID", insertable = false, updatable = false)
    SdcSchoolCollectionEntity sdcSchoolCollectionEntity;

    @EqualsAndHashCode.Exclude
    @ToString.Exclude
    @OneToMany(mappedBy = "sdcSchoolCollectionStudentEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, targetEntity = SdcSchoolCollectionStudentValidationIssueEntity.class)
    Set<SdcSchoolCollectionStudentValidationIssueEntity> sdcStudentValidationIssueEntities;

    public UUID getSdcSchoolCollectionEntitySchoolID() {
        return this.sdcSchoolCollectionEntity != null ? this.sdcSchoolCollectionEntity.getSchoolID() : null;
    }
}
