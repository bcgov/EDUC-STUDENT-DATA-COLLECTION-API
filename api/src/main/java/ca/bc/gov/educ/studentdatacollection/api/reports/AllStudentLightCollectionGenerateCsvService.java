package ca.bc.gov.educ.studentdatacollection.api.reports;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.EnrolledProgramCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
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

    public DownloadableReportResponse generateFromSdcSchoolCollectionID(UUID sdcSchoolcollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightBySchoolCollectionID(sdcSchoolcollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous  Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC - Career Technical C.", "Special Ed Category")
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
            downloadableReport.setReportType("ALL_STUDENT_SCHOOL_CSV");
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFromSdcDistrictCollectionID(UUID sdcDistrictcollectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightByDistrictCollectionId(sdcDistrictcollectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("School ID", "School Name", "P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous  Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Years in ELL", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC - Career Technical C.", "Special Ed Category")
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
            downloadableReport.setReportType("ALL_STUDENT_DIS_CSV");
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private List<Object> prepareStudentDataForCsv(SdcSchoolCollectionStudentLightEntity student, Boolean isDistrict) {
        List<Object> csvRowData = new ArrayList<>();
        if (Boolean.TRUE.equals(isDistrict)) {
            UUID schoolID = student.getSdcSchoolCollectionEntitySchoolID();
            Optional<School> school = restUtils.getSchoolBySchoolID(schoolID.toString());
            String schoolName = school.map(School::getDisplayName).orElse("No School Name Found");
            csvRowData.add(schoolID);
            csvRowData.add(schoolName);
        }
        String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
        String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
        String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "Y" : "N";
        String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "Y" : "N";
        String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "Y" : "N";
        Set<String> enrolledProgramCodesSet = parseEnrolledProgramCodes(student.getEnrolledProgramCodes());


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
                    Boolean.TRUE.equals(student.getIsAdult()) ? "Y" : "N",
                    Boolean.TRUE.equals(student.getIsGraduated()) ? "Y" : "N",
                    feePayer,
                    refugee,
                    student.getNativeAncestryInd(),
                    ordinarilyResidentOnReserve,
                    student.getBandCode(),
                    student.getHomeLanguageSpokenCode(),
                    student.getNumberOfCourses(),
                    student.getSupportBlocks(),
                    student.getOtherCourses(),
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.PROGRAMME_FRANCOPHONE.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.CORE_FRENCH.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.EARLY_FRENCH_IMMERSION.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.LATE_FRENCH_IMMERSION.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.ENGLISH_LANGUAGE_LEARNING.getCode()) ? "Y" : "N",
                    student.getYearsInEll(),
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.ABORIGINAL_LANGUAGE.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.ABORIGINAL_SUPPORT.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.OTHER_APPROVED_NATIVE.getCode()) ? "Y" : "N",
                    student.getCareerProgramCode(),
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.CAREER_PREPARATION.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.COOP.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.APPRENTICESHIP.getCode()) ? "Y" : "N",
                    enrolledProgramCodesSet.contains(EnrolledProgramCodes.CAREER_TECHNICAL_CENTER.getCode()) ? "Y" : "N",
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

    public Set<String> parseEnrolledProgramCodes(String enrolledProgramCodes) {
        if (StringUtils.isEmpty(enrolledProgramCodes)) {
            return Collections.emptySet();
        }
        return IntStream.iterate(0, i -> i < enrolledProgramCodes.length(), i -> i + 2)
                .mapToObj(i -> enrolledProgramCodes.substring(i, Math.min(i + 2, enrolledProgramCodes.length())))
                .collect(Collectors.toSet());
    }
}
