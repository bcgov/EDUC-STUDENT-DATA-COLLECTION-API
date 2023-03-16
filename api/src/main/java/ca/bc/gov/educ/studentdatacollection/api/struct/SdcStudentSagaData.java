package ca.bc.gov.educ.studentdatacollection.api.struct;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * The type Pen request batch student saga data.
 */
@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SdcStudentSagaData {
  /**
   * The Pen request batch student id.
   */
  @NotNull(message = "penRequestBatchStudentID cannot be null")
  UUID penRequestBatchStudentID;
  /**
   * The Pen request batch id.
   */
  @NotNull(message = "penRequestBatchID cannot be null")
  UUID penRequestBatchID;
  /**
   * The Pen request batch student status code.
   */
  String penRequestBatchStudentStatusCode;

  /**
   * The mincode.
   */
  String mincode;
  /**
   * The Local id.
   */
  String localID;
  /**
   * The Submitted pen.
   */
  String submittedPen;
  /**
   * The Legal first name.
   */
  String legalFirstName;
  /**
   * The Legal middle names.
   */
  String legalMiddleNames;
  /**
   * The Legal last name.
   */
  String legalLastName;
  /**
   * The Usual first name.
   */
  String usualFirstName;
  /**
   * The Usual middle names.
   */
  String usualMiddleNames;
  /**
   * The Usual last name.
   */
  String usualLastName;
  /**
   * The Dob.
   */
  String dob;
  /**
   * The Gender code.
   */
  String genderCode;
  /**
   * The Grade code.
   */
  String gradeCode;
  /**
   * The Postal code.
   */
  String postalCode;
  /**
   * The Assigned pen.
   */
  String assignedPEN;
  /**
   * The Student id.
   */
  String studentID;

  /**
   * The Create user.
   */
  String createUser;

  /**
   * The Update user.
   */
  String updateUser;
  /**
   * The Pen match result.
   */
  String matchAlgorithmStatusCode;

  /**
   * The Questionable match student id.
   */
  UUID questionableMatchStudentId;

  /**
   * The Info request.
   */
  String infoRequest;
  /**
   * The Pen match result.
   */
  PenMatchResult penMatchResult;

  /**
   * The Generate pen.
   * this is important in replay scenario when pod dies, rather than generating a new PEN use the PEN it was already generated.
   */
  String generatedPEN;

  /**
   * The Is pen match results processed.
   * this is important in replay scenario when pod dies
   */
  Boolean isPENMatchResultsProcessed;
}
