package ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder
@JsonIgnoreProperties(ignoreUnknown = true)
public class School extends BaseRequest implements Serializable {
  /**
   * The constant serialVersionUID.
   */
  private static final long serialVersionUID = 1L;

  private String schoolId;
  @NotNull(message = "districtId can not be null.")
  private String districtId;

  private String mincode;

  private String independentAuthorityId;

  @Size(max = 5)
  private String schoolNumber;

  @Size(max = 10)
  @Pattern(regexp = "^$|\\d{10}", message = "Invalid phone number format")
  private String faxNumber;

  @Size(max = 10)
  @Pattern(regexp = "^$|\\d{10}", message = "Invalid phone number format")
  private String phoneNumber;

  @Size(max = 255)
  @Email(message = "Email address should be a valid email address")
  private String email;

  @Size(max = 255)
  private String website;

  @Size(max = 10)
  @NotNull(message = "schoolReportingRequirementCode cannot be null")
  private String schoolReportingRequirementCode;

  @Size(max = 255)
  @NotNull(message = "displayName cannot be null")
  private String displayName;

  @Size(max = 255)
  private String displayNameNoSpecialChars;

  @Size(max = 10)
  @NotNull(message = "schoolOrganizationCode cannot be null")
  private String schoolOrganizationCode;

  @Size(max = 10)
  @NotNull(message = "schoolCategoryCode cannot be null")
  private String schoolCategoryCode;

  @Size(max = 10)
  @NotNull(message = "facilityTypeCode cannot be null")
  private String facilityTypeCode;

  private String openedDate;

  private String closedDate;

  private Boolean canIssueTranscripts;

  private Boolean canIssueCertificates;

  @Valid
  private List<SchoolContact> contacts;

  @Valid
  private List<SchoolAddress> addresses;

  @Valid
  private List<SchoolGrade> grades;

  @Valid
  private List<IndependentSchoolFundingGroup> schoolFundingGroups;

  @Valid
  private List<NeighborhoodLearning> neighborhoodLearning;

}
