package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

public final class URL {

  private URL(){
  }

  public static final String BASE_URL="/api/v1/student-data-collection";
  public static final String BASE_URL_COLLECTION="/api/v1/student-data-collection/collection";
  public static final String BASE_URL_DISTRICT_COLLECTION="/api/v1/student-data-collection/sdcDistrictCollection";
  public static final String BASE_DISTRICT_HEADCOUNTS = "/api/v1/student-data-collection/headcounts";
  public static final String BASE_MINISTRY_HEADCOUNTS = "/api/v1/student-data-collection/ministryHeadcounts";
  public static final String BASE_URL_SCHOOL_COLLECTION="/api/v1/student-data-collection/sdcSchoolCollection";
  public static final String BASE_URL_SCHOOL_COLLECTION_STUDENT="/api/v1/student-data-collection/sdcSchoolCollectionStudent";
  public static final String BASE_URL_REPORT_GENERATION="/api/v1/student-data-collection/reportGeneration";
  public static final String BASE_URL_SCHOOL_FUNDING="/api/v1/student-data-collection/schoolFundingGroupSnapshot";
  public static final String BASE_URL_DUPLICATE = "/api/v1/student-data-collection/sdc-duplicate";
  public static final String ENROLLED_PROGRAM_CODES="/enrolled-program-codes";
  public static final String CAREER_PROGRAM_CODES="/career-program-codes";
  public static final String HOME_LANGUAGE_SPOKEN_CODES="/home-language-codes";
  public static final String BAND_CODES="/band-codes";
  public static final String FUNDING_CODES="/funding-codes";
  public static final String GRADE_CODES="/grade-codes";
  public static final String PAGINATED = "/paginated";
  public static final String PAGINATED_SHALLOW = "/paginated-shallow";
  public static final String PAGINATED_SLD_HISTORY = "/paginated-sld-history";
  public static final String PAGINATED_SLICE = "/paginated-slice";
  public static final String SPED_CODES="/specialEducation-codes";
  public static final String GENDER_CODES="/gender-codes";
  public static final String VALIDATION_ISSUE_TYPE_CODES = "/validation-issue-type-codes";
  public static final String ERROR_WARNING_COUNT = "/stats/error-warning-count";
  public static final String FUNDING_GROUP_CODES = "/funding-group-codes";
  public static final String ZERO_FTE_REASON_CODES = "/zero-fte-reason-codes";
  public static final String HEADCOUNTS = "/headcounts";
  public static final String COLLECTION_TYPE_CODES = "/collection-type-codes";
  public static final String PROGRAM_ELIGIBILITY_ISSUE_CODES = "/program-eligibility-issue-codes";
  public static final String DUPLICATE_RESOLUTION_CODES = "/duplicate-resolution-codes";
  public static final String PROGRAM_DUPLICATE_TYPE_CODES = "/program-duplicate-type-codes";
  public static final String SDC_SCHOOL_COLLECTION_STATUS_CODES = "/school-collection-status-codes";
  public static final String SDC_DISTRICT_COLLECTION_STATUS_CODES = "/district-collection-status-codes";

}
