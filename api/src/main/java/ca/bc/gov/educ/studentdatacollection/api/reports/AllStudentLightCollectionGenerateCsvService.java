package ca.bc.gov.educ.studentdatacollection.api.reports;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
@Slf4j
public class AllStudentLightCollectionGenerateCsvService {
    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;
    private final RestUtils restUtils;

    // SCHOOL COMMON COLUMN HEADERS
    private static final String SCHOOL_CODE = "School Code";
    private static final String SCHOOL_NAME = "School Name";
    private static final String FACILITY_CODE = "Facility Code";

    // ALL STUDENT COMMON COLUMN HEADERS
    private static final String PEN = "PEN";
    private static final String LEGAL_NAME = "Legal Name";
    private static final String USUAL_NAME = "Usual Name";
    private static final String FTE = "FTE";
    private static final String PROGRAM_ELIGIBLE = "Program Eligible";
    private static final String LOCAL_ID = "Local ID";
    private static final String ADULT = "Adult";
    private static final String GRADUATE = "Graduate";
    private static final String GRADE = "Grade";
    private static final String FUNDING_CODE = "Funding Code";

    // OTHER HEADERS
    private static final String GENDER = "Gender";
    private static final String BIRTH_DATE = "Birth Date";
    private static final String POSTAL_CODE = "Postal Code";
    private static final String FEE_PAYER = "Fee Payer";
    private static final String INDIGENOUS_OTHER = "Indigenous Other";
    private static final String INDIGENOUS_ANCESTRY = "Indigenous Ancestry";

    // WARNING MESSAGES
    private static final String NO_SCHOOL_CODE_FOUND = "No School Code Found";
    private static final String NO_SCHOOL_NAME_FOUND = "No School Name Found";
    private static final String NO_FACILITY_TYPE_FOUND = "No Facility Type Found";

    public AllStudentLightCollectionGenerateCsvService(SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService, RestUtils restUtils) {
        this.sdcSchoolCollectionStudentSearchService = sdcSchoolCollectionStudentSearchService;
        this.restUtils = restUtils;
    }

    public DownloadableReportResponse generateErrorWarnInfoReportFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsWithErrorsWarningInfoBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Errors & Warnings", PEN, LEGAL_NAME, USUAL_NAME, BIRTH_DATE, GENDER, POSTAL_CODE, LOCAL_ID, GRADE, FTE, ADULT, GRADUATE, FEE_PAYER,
                        "Refugee", INDIGENOUS_ANCESTRY, "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", INDIGENOUS_OTHER,
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC - Career Technical C.", "Inclusive Ed Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentEntity student : entities) {
                List<Object> csvRowData = prepareStudentDataWithErrorsAndWarningsForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_ERRORS_WARNS_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, BIRTH_DATE, GENDER, POSTAL_CODE, LOCAL_ID, GRADE, FTE, ADULT, GRADUATE, FEE_PAYER,
                        "Refugee", INDIGENOUS_ANCESTRY, "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", INDIGENOUS_OTHER,
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC - Career Technical C.", "Inclusive Ed Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                List<Object> csvRowData = prepareStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFrenchFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllFrenchStudentsLightBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "French Program")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareFrenchStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_FRENCH_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateCareerFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllCareerStudentsLightBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Career Program", "Career Code")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareCareerStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_CAREER_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateIndigenousFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllIndigenousStudentsLightBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Indigenous Ancestry", "Band Code", "Indigenous Support Program")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareIndigenousStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_INDIGENOUS_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateInclusiveFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllInclusiveEdStudentsLightBySchoolCollectionId(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Inclusive Education Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                List<Object> csvRowData = prepareInclusiveStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_INCLUSIVE_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateEllFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllEllStudentsLightBySchoolCollectionId(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Language Program", "Years in ELL")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareEllStudentDataForCsv(student, false);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SchoolReportTypeCode.ALL_STUDENT_ELL_SCHOOL_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateErrorWarnInfoReportFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsWithErrorsWarningInfoByDistrictCollectionID(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, "Facility Type","Errors & Warnings", PEN, LEGAL_NAME, USUAL_NAME, BIRTH_DATE, GENDER, POSTAL_CODE, LOCAL_ID, GRADE, FTE, ADULT, GRADUATE, FEE_PAYER,
                        "Refugee", "Indigenous Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Lang", "# Courses", "# Support Blocks", "# Other Courses",
                        "Prog Franco", "Core French", "Early Immer", "Late Immer", "ELL", "ELL-yrs", "Indigenous Culture/Lang", "Indigenous Support", INDIGENOUS_OTHER,
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC", "Inclusive Ed Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentEntity student : entities) {
                List<Object> csvRowData = prepareStudentDataWithErrorsAndWarningsForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_ERRORS_WARNS_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, "Facility Type", PEN, LEGAL_NAME, USUAL_NAME, BIRTH_DATE, GENDER, POSTAL_CODE, LOCAL_ID, GRADE, FTE, ADULT, GRADUATE, FEE_PAYER,
                        "Refugee", "Indigenous Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Lang", "# Courses", "# Support Blocks", "# Other Courses",
                        "Prog Franco", "Core French", "Early Immer", "Late Immer", "ELL", "ELL-yrs", "Indigenous Culture/Lang", "Indigenous Support", INDIGENOUS_OTHER,
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC", "Inclusive Ed Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                List<Object> csvRowData = prepareStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFrenchFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllFrenchStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, FACILITY_CODE, PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "French Program")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareFrenchStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_FRENCH_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateCareerFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllCareerStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, FACILITY_CODE, PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Career Program", "Career Code")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareCareerStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_CAREER_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateIndigenousFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllIndigenousStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, FACILITY_CODE, PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE,
                        "Indigenous Ancestry", "Band Code", "Indigenous Support Program")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareIndigenousStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_INDIGENOUS_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateInclusiveFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllInclusiveEdStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, FACILITY_CODE, PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Inclusive Education Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                List<Object> csvRowData = prepareInclusiveStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_INCLUSIVE_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateEllFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity> entities = sdcSchoolCollectionStudentSearchService.findAllEllStudentsLightByDistrictCollectionId(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_CODE, SCHOOL_NAME, FACILITY_CODE, PEN, LEGAL_NAME, USUAL_NAME, FTE, PROGRAM_ELIGIBLE, LOCAL_ID,  ADULT, GRADUATE, GRADE, FUNDING_CODE, "Language Program", "Years in ELL")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student : entities) {
                List<Object> csvRowData = prepareEllStudentDataForCsv(student, true);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(DistrictReportTypeCode.ALL_STUDENT_ELL_DIS_CSV.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private List<Object> prepareStudentDataWithErrorsAndWarningsForCsv(SdcSchoolCollectionStudentEntity student, boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollection().getSchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;
            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
        String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "1" : "";
        String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "1" : "";
        String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "1" : "";
        Map<String, String> enrolledProgramCodesMap = parseEnrolledProgramCodes(student.getEnrolledProgramCodes(), "1");

        csvRowData.addAll(Arrays.asList(
                getErrorsAndWarningString(student),
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getDob(),
                student.getGender(),
                student.getPostalCode(),
                student.getLocalID(),
                student.getEnrolledGradeCode(),
                student.getFte(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                feePayer,
                refugee,
                convertToBinary(student.getNativeAncestryInd()),
                ordinarilyResidentOnReserve,
                student.getBandCode(),
                student.getHomeLanguageSpokenCode(),
                student.getNumberOfCoursesDec() != null ? student.getNumberOfCoursesDec() : "",
                student.getSupportBlocks(),
                student.getOtherCourses(),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.CORE_FRENCH.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.EARLY_FRENCH_IMMERSION.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.LATE_FRENCH_IMMERSION.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode()),
                student.getYearsInEll(),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.ABORIGINAL_LANGUAGE.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.ABORIGINAL_SUPPORT.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.OTHER_APPROVED_NATIVE.getCode()),
                student.getCareerProgramCode(),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.CAREER_PREPARATION.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.COOP.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.YOUTH_WORK_IN_TRADES.getCode()),
                enrolledProgramCodesMap.get(EnrolledProgramCodes.CAREER_TECHNICAL_CENTER.getCode()),
                student.getSpecialEducationCategoryCode()
        ));
        return csvRowData;
    }

    private String getErrorsAndWarningString(SdcSchoolCollectionStudentEntity student){
        StringBuilder builder = new StringBuilder();
        var errorAndWarnSet = new HashSet<String>();
        student.getSDCStudentValidationIssueEntities().forEach(sdcSchoolCollectionStudentValidationIssueEntity -> {
            if(!errorAndWarnSet.contains(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode())) {
                var optIssueCode = StudentValidationIssueSeverityCode.findByValue(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueSeverityCode());
                var issueTypeCode = StudentValidationIssueTypeCode.findByValue(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode());
                builder.append(optIssueCode.isPresent() ? optIssueCode.get().getLabel() : "N/A");
                builder.append(" - ");
                builder.append(issueTypeCode != null ? issueTypeCode.getMessage() : "N/A");
                builder.append("\n");
                errorAndWarnSet.add(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode());
            }
        });
        if(!builder.isEmpty()) {
            return StringUtils.removeEnd(builder.toString(), "\n");
        }
        return "";
    }

    private List<Object> prepareStudentDataForCsv(SdcSchoolCollectionStudentLightEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;
            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
        String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "1" : "";
        String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "1" : "";
        String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "1" : "";
        Map<String, String> enrolledProgramCodesMap = parseEnrolledProgramCodes(student.getEnrolledProgramCodes(), "1");

        csvRowData.addAll(Arrays.asList(
                    student.getStudentPen(),
                    legalFullName,
                    usualFullName,
                    student.getDob(),
                    student.getGender(),
                    student.getPostalCode(),
                    student.getLocalID(),
                    student.getEnrolledGradeCode(),
                    student.getFte(),
                    Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                    Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                    feePayer,
                    refugee,
                    convertToBinary(student.getNativeAncestryInd()),
                    ordinarilyResidentOnReserve,
                    student.getBandCode(),
                    student.getHomeLanguageSpokenCode(),
                    student.getNumberOfCoursesDec() != null ? student.getNumberOfCoursesDec() : "",
                    student.getSupportBlocks(),
                    student.getOtherCourses(),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.CORE_FRENCH.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.EARLY_FRENCH_IMMERSION.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.LATE_FRENCH_IMMERSION.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode()),
                    student.getYearsInEll(),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.ABORIGINAL_LANGUAGE.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.ABORIGINAL_SUPPORT.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.OTHER_APPROVED_NATIVE.getCode()),
                    student.getCareerProgramCode(),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.CAREER_PREPARATION.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.COOP.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.YOUTH_WORK_IN_TRADES.getCode()),
                    enrolledProgramCodesMap.get(EnrolledProgramCodes.CAREER_TECHNICAL_CENTER.getCode()),
                    student.getSpecialEducationCategoryCode()
        ));
        return csvRowData;
    }

    private List<Object> prepareFrenchStudentDataForCsv(SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;

            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

        csvRowData.addAll(Arrays.asList(
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getFte(),
                StringUtils.isBlank(student.getFrenchProgramNonEligReasonCode()) ? "1" : "",
                student.getLocalID(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                student.getEnrolledGradeCode(),
                StringUtils.isBlank(student.getSchoolFundingCode()) ? "" : student.getSchoolFundingCode().replaceAll("(.{2})(?=.)","$1,"),
                StringUtils.isBlank(student.getEnrolledProgramCodes())
                        ? ""
                        : Arrays.stream(student.getEnrolledProgramCodes().split("(?<=\\G.{2})"))
                        .filter(code -> EnrolledProgramCodes.getFrenchProgramCodes().contains(code))
                        .collect(Collectors.joining(","))
        ));
        return csvRowData;
    }

    private List<Object> prepareCareerStudentDataForCsv(SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;

            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

        csvRowData.addAll(Arrays.asList(
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getFte(),
                StringUtils.isBlank(student.getFrenchProgramNonEligReasonCode()) ? "1" : "",
                student.getLocalID(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                student.getEnrolledGradeCode(),
                StringUtils.isBlank(student.getSchoolFundingCode()) ? "" : student.getSchoolFundingCode().replaceAll("(.{2})(?=.)","$1,"),
                StringUtils.isBlank(student.getEnrolledProgramCodes())
                        ? ""
                        : Arrays.stream(student.getEnrolledProgramCodes().split("(?<=\\G.{2})"))
                        .filter(code -> EnrolledProgramCodes.getCareerProgramCodes().contains(code))
                        .collect(Collectors.joining(",")),
                student.getCareerProgramCode()
        ));
        return csvRowData;
    }

    private List<Object> prepareIndigenousStudentDataForCsv(SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;

            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

        csvRowData.addAll(Arrays.asList(
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getFte(),
                StringUtils.isBlank(student.getFrenchProgramNonEligReasonCode()) ? "1" : "",
                student.getLocalID(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                student.getEnrolledGradeCode(),
                StringUtils.isBlank(student.getSchoolFundingCode()) ? "" : student.getSchoolFundingCode().replaceAll("(.{2})(?=.)","$1,"),
                student.getNativeAncestryInd(),
                student.getBandCode(),
                StringUtils.isBlank(student.getEnrolledProgramCodes())
                        ? ""
                        : Arrays.stream(student.getEnrolledProgramCodes().split("(?<=\\G.{2})"))
                        .filter(code -> EnrolledProgramCodes.getIndigenousProgramCodes().contains(code))
                        .collect(Collectors.joining(","))
        ));
        return csvRowData;
    }

    private List<Object> prepareInclusiveStudentDataForCsv(SdcSchoolCollectionStudentLightEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;

            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

        csvRowData.addAll(Arrays.asList(
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getFte(),
                StringUtils.isBlank(student.getSpecialEducationNonEligReasonCode()) ? "1" : "",
                student.getLocalID(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                student.getEnrolledGradeCode(),
                StringUtils.isBlank(student.getSchoolFundingCode()) ? "" : student.getSchoolFundingCode().replaceAll("(.{2})(?=.)","$1,"),
                student.getSpecialEducationCategoryCode()
        ));
        return csvRowData;
    }

    private List<Object> prepareEllStudentDataForCsv(SdcSchoolCollectionStudentLightWithEnrolledProgramCodesEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            var facilityType = restUtils.getFacilityTypeCode(school.get().getFacilityTypeCode());

            String schoolCode = school.isPresent() ? school.get().getMincode() : NO_SCHOOL_CODE_FOUND;
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse(NO_SCHOOL_NAME_FOUND);
            String finalFacilityType = facilityType.isPresent() ? facilityType.get().getLabel() : NO_FACILITY_TYPE_FOUND;

            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(finalFacilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

        csvRowData.addAll(Arrays.asList(
                student.getStudentPen(),
                legalFullName,
                usualFullName,
                student.getFte(),
                StringUtils.isBlank(student.getFrenchProgramNonEligReasonCode()) ? "1" : "",
                student.getLocalID(),
                Boolean.TRUE.equals(student.getIsAdult()) ? "1" : "",
                Boolean.TRUE.equals(student.getIsGraduated()) ? "1" : "",
                student.getEnrolledGradeCode(),
                StringUtils.isBlank(student.getSchoolFundingCode()) ? "" : student.getSchoolFundingCode().replaceAll("(.{2})(?=.)","$1,"),
                StringUtils.isBlank(student.getEnrolledProgramCodes())
                        ? ""
                        : Arrays.stream(student.getEnrolledProgramCodes().split("(?<=\\G.{2})"))
                        .filter(code -> EnrolledProgramCodes.getELLCodes().contains(code))
                        .collect(Collectors.joining(",")),
                student.getYearsInEll()
        ));
        return csvRowData;
    }

    public String formatFullName(String firstName, String middleNames, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (StringUtils.isNotBlank(lastName)) {
            fullName.append(lastName);
        }

        if (StringUtils.isNotBlank(firstName)) {
            if (!fullName.isEmpty()) {
                fullName.append(", ");
            }
            fullName.append(firstName);
        }

        if (StringUtils.isNotBlank(middleNames)) {
            if (!fullName.isEmpty()) {
                if (StringUtils.isNotBlank(firstName)) {
                    fullName.append(" ");
                } else {
                    fullName.append(", ");
                }
            }
            fullName.append(middleNames);
        }

        return fullName.toString().trim();
    }

    public Map<String, String> parseEnrolledProgramCodes(String enrolledProgramCodes, String displayValue) {
        Map<String, String> codesMap = Arrays.stream(new String[] {
                EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode(),
                EnrolledProgramCodes.CORE_FRENCH.getCode(),
                EnrolledProgramCodes.EARLY_FRENCH_IMMERSION.getCode(),
                EnrolledProgramCodes.LATE_FRENCH_IMMERSION.getCode(),
                EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode(),
                EnrolledProgramCodes.ABORIGINAL_LANGUAGE.getCode(),
                EnrolledProgramCodes.ABORIGINAL_SUPPORT.getCode(),
                EnrolledProgramCodes.OTHER_APPROVED_NATIVE.getCode(),
                EnrolledProgramCodes.CAREER_PREPARATION.getCode(),
                EnrolledProgramCodes.COOP.getCode(),
                EnrolledProgramCodes.YOUTH_WORK_IN_TRADES.getCode(),
                EnrolledProgramCodes.CAREER_TECHNICAL_CENTER.getCode()
        }).collect(Collectors.toMap(code -> code, code -> ""));

        if (enrolledProgramCodes != null && !enrolledProgramCodes.isEmpty()) {
            IntStream.iterate(0, i -> i < enrolledProgramCodes.length(), i -> i + 2)
                    .mapToObj(i -> enrolledProgramCodes.substring(i, Math.min(i + 2, enrolledProgramCodes.length())))
                    .forEach(code -> {
                        if (codesMap.containsKey(code)) {
                            codesMap.put(code, displayValue);
                        }
                    });
        }

        return codesMap;
    }

    public String convertToBinary(String code) {
        if (code == null || code.equals("N")) {
            return "";
        }
        if (code.equals("Y")) {
            return "1";
        }
        return code;
    }
}
