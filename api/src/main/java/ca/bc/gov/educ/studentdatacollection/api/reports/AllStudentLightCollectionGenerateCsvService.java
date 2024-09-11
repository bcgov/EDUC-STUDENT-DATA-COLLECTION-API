package ca.bc.gov.educ.studentdatacollection.api.reports;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueSeverityCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.StudentValidationIssueTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.DistrictReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
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

    public AllStudentLightCollectionGenerateCsvService(SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService, RestUtils restUtils) {
        this.sdcSchoolCollectionStudentSearchService = sdcSchoolCollectionStudentSearchService;
        this.restUtils = restUtils;
    }

    public DownloadableReportResponse generateErrorWarnInfoReportFromSdcSchoolCollectionID(UUID sdcSchoolCollectionID) {
        List<SdcSchoolCollectionStudentEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsWithErrorsWarningInfoBySchoolCollectionID(sdcSchoolCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("Errors & Warnings", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous  Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
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
                .setHeader("P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous  Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
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

    public DownloadableReportResponse generateErrorWarnInfoReportFromSdcDistrictCollectionID(UUID sdcDistrictCollectionID) {
        List<SdcSchoolCollectionStudentEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsWithErrorsWarningInfoByDistrictCollectionID(sdcDistrictCollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("School Code", "School Name", "Facility Type","Errors & Warnings", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Lang", "# Courses", "# Support Blocks", "# Other Courses",
                        "Prog Franco", "Core French", "Early Immer", "Late Immer", "ELL", "ELL-yrs", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
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
                .setHeader("School Code", "School Name", "Facility Type", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Lang", "# Courses", "# Support Blocks", "# Other Courses",
                        "Prog Franco", "Core French", "Early Immer", "Late Immer", "ELL", "ELL-yrs", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
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

    private List<Object> prepareStudentDataWithErrorsAndWarningsForCsv(SdcSchoolCollectionStudentEntity student, boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollection().getSchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            String schoolCode = school.isPresent() ? school.get().getMincode() : "No School Code Found";
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse("No School Name Found");
            String facilityType = school.isPresent() ? school.get().getFacilityTypeCode() : "No Facility Type Found";
            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(facilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
        String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "1" : "";
        String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "1" : "";
        String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "1" : "";
        Map<String, String> enrolledProgramCodesMap = parseEnrolledProgramCodes(student.getEnrolledProgramCodes());

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
                student.getNumberOfCourses(),
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
        student.getSDCStudentValidationIssueEntities().forEach(sdcSchoolCollectionStudentValidationIssueEntity -> {
            var errorAndWarnSet = new HashSet<String>();
            if(!errorAndWarnSet.contains(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode())) {
                var optIssueCode = StudentValidationIssueSeverityCode.findByValue(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueSeverityCode());
                var issueTypeCode = StudentValidationIssueTypeCode.findByValue(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode());
                builder.append(optIssueCode.isPresent() ? optIssueCode.get().getCode() : "N/A");
                builder.append(" - ");
                builder.append(issueTypeCode != null ? issueTypeCode.getMessage() : "N/A");
                builder.append("\n");
                errorAndWarnSet.add(sdcSchoolCollectionStudentValidationIssueEntity.getValidationIssueCode());
            }
        });
        if(!builder.isEmpty()){
            return builder.toString().substring(0, builder.length()-2);
        }
        return "";
    }

    private List<Object> prepareStudentDataForCsv(SdcSchoolCollectionStudentLightEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<SchoolTombstone> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            String schoolCode = school.isPresent() ? school.get().getMincode() : "No School Code Found";
            String schoolName = school.map(SchoolTombstone::getDisplayName).orElse("No School Name Found");
            String facilityType = school.isPresent() ? school.get().getFacilityTypeCode() : "No Facility Type Found";
            csvRowData.add(schoolCode);
            csvRowData.add(schoolName);
            csvRowData.add(facilityType);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
        String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "1" : "";
        String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "1" : "";
        String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "1" : "";
        Map<String, String> enrolledProgramCodesMap = parseEnrolledProgramCodes(student.getEnrolledProgramCodes());

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
                    student.getNumberOfCourses(),
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

    public Map<String, String> parseEnrolledProgramCodes(String enrolledProgramCodes) {
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
                            codesMap.put(code, "1");
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
