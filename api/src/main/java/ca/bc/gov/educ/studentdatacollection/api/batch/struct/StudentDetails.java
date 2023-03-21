package ca.bc.gov.educ.studentdatacollection.api.batch.struct;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The type Student details.
 *
 * @author OM The type Student details.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentDetails {

  private String transactionCode; // TRANSACTION_CODE	3	0	Always "SRM"

  private String localStudentID; //LOCAL_STUDENT_ID	12	3	The student identifier that is assigned by the school.

  private String pen; // PEN	10	15	The Personal Education Number that is assigned by the Ministry. Left-justified, consisting of 9 digits followed by a blank.

  private String legalSurname; // LEGAL_SURNAME	25	25	The legal name is the name appearing on the student's birth certificate or a change of name document.

  private String legalGivenName; // LEGAL_GIVEN_NAME	25	50	The first or given legal name of the student.

  private String legalMiddleName; // LEGAL_MIDDLE_NAME	25	75	The second or middle legal name of the student.

  private String usualSurname; // USUAL_SURNAME	25	100	The surname that the student prefers to be known by.

  private String usualGivenName; // USUAL_GIVEN_NAME	25	125	The first or given name that a student prefers to be known by.

  private String usualMiddleName; // USUAL_MIDDLE_NAME	25	150	The second or middle name that the student prefers to be known by.

  private String birthDate; //  BIRTH DATE	8	175	The birth date of the student. Format: YYYYMMDD

  private String gender; // GENDER	1	183	The gender of the student (M or F)

  private String specialEducationCategory; // SPECIAL_ED_CATEGORY	1		184

  private String unusedBlock1; // UNUSED	3	185

  private String schoolFundingCode; // SCHOOL_FUNDING_CODE	2		188

  private String nativeAncestryIndicator; // NATIVE_INDIAN_ANCESTRY_IND	1		190

  private String homeSpokenLanguageCode; // HOME_LANGUAGE_SPOKEN_CODE	3		191

  private String unusedBlock2; // UNUSED 4		194

  private String otherCourses; // OTHER_COURSES	1		198

  private String supportBlocks; // SUPPORT BLOCKS	1		199

  private String enrolledGradeCode; // ENROLLED_GRADE_CODE	2		200

  private String enrolledProgramCodes; // ENROLLED_PROGRAM_CODE	2		202

  private String careerProgramCode; // CAREER_PROGRAM_CODE	2		218

  private String numberOfCourses; // NUMBER_OF_COURSES	4		220

  private String bandCode; // BAND_CODE	4		224

  private String postalCode; // POSTAL_CODE	6	228

}
