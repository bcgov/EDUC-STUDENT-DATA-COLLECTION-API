package ca.bc.gov.educ.studentdatacollection.api.reports;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Service
@Slf4j
public class AllStudentLightCollectionGenerateCsvService {
    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;

    public AllStudentLightCollectionGenerateCsvService(SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService) {
        this.sdcSchoolCollectionStudentSearchService = sdcSchoolCollectionStudentSearchService;
    }

    public DownloadableReportResponse generate(UUID collectionID) {
        List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightSynchronous(collectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader("P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate", "Fee Payer",
                        "Refugee", "Indigenous  Ancestry", "Ordinarily Resident on Reserve", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses",
                        "Programme Francophone", "Core French", "Early Immersion", "Late Immersion", "ELL", "Indigenous Culture/Lang", "Indigenous Support", "Indigenous Other",
                        "Career Prog", "Career Prep", "Coop", "Apprentice", "CTC - Career Technical C.", "Special Ed Category")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
                String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());
                String feePayer = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("14") ? "Y" : "N";
                String refugee = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("16") ? "Y" : "N";
                String ordinarilyResidentOnReserve = student.getSchoolFundingCode() != null && student.getSchoolFundingCode().contentEquals("20") ? "Y" : "N";

                String enrolledProgramCodes = student.getEnrolledProgramCodes();
                Set<String> enrolledProgramCodesSet = (enrolledProgramCodes != null && !enrolledProgramCodes.isEmpty())
                        ? IntStream.iterate(0, i -> i < enrolledProgramCodes.length(), i -> i + 2)
                        .mapToObj(i -> enrolledProgramCodes.substring(i, Math.min(i + 2, enrolledProgramCodes.length())))
                        .collect(Collectors.toSet())
                        : Collections.emptySet();

                List<? extends Serializable> csvRow = Arrays.asList(
                        student.getStudentPen(),
                        legalFullName,
                        usualFullName,
                        student.getDob(),
                        student.getGender(),
                        student.getPostalCode(),
                        student.getLocalID(),
                        student.getEnrolledGradeCode(),
                        student.getFte(),
                        student.getIsAdult(),
                        student.getIsGraduated(),
                        feePayer,
                        refugee,
                        student.getNativeAncestryInd(),
                        ordinarilyResidentOnReserve,
                        student.getBandCode(),
                        student.getHomeLanguageSpokenCode(),
                        student.getNumberOfCourses(),
                        student.getSupportBlocks(),
                        student.getOtherCourses(),
                        enrolledProgramCodesSet.contains(ProgramCode.PROGRAMME_FRANCOPHONE.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.CORE_FRENCH.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.EARLY_IMMERSION.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.LATE_IMMERSION.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.ELL.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.INDIGENOUS_CULTURE_LANG.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.INDIGENOUS_SUPPORT.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.INDIGENOUS_OTHER.getCode()) ? "Y" : "N",
                        student.getCareerProgramCode(),
                        enrolledProgramCodesSet.contains(ProgramCode.CAREER_PREP.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.COOP.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.APPRENTICE.getCode()) ? "Y" : "N",
                        enrolledProgramCodesSet.contains(ProgramCode.CTC_CAREER_TECHNICAL_C.getCode()) ? "Y" : "N",
                        student.getSpecialEducationCategoryCode()
                );
                csvPrinter.printRecord(csvRow);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType("ALL_STUDENT_CSV");
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new AllStudentLightCollectionGenerateCsvService.CsvGenerationException("Failed to generate CSV", e);
        }
    }

    private String formatFullName(String firstName, String middleNames, String lastName) {
        StringBuilder fullName = new StringBuilder();

        if (StringUtils.isNotBlank(firstName)) {
            fullName.append(firstName);
        }

        if (StringUtils.isNotBlank(middleNames)) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(middleNames);
        }

        if (StringUtils.isNotBlank(lastName)) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(lastName);
        }

        return fullName.toString().trim();
    }

    public static class CsvGenerationException extends RuntimeException {
        public CsvGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @Getter
    public enum ProgramCode {
        PROGRAMME_FRANCOPHONE("05"),
        CORE_FRENCH("08"),
        EARLY_IMMERSION("11"),
        LATE_IMMERSION("14"),
        ELL("17"),
        INDIGENOUS_CULTURE_LANG("29"),
        INDIGENOUS_SUPPORT("33"),
        INDIGENOUS_OTHER("36"),
        CAREER_PREP("40"),
        COOP("41"),
        APPRENTICE("42"),
        CTC_CAREER_TECHNICAL_C("43");

        private final String code;

        ProgramCode(String code) {
            this.code = code;
        }

    }
}
