package ca.bc.gov.educ.studentdatacollection.api.reports;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;


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
                .setHeader("P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate",
                        "Native Ancestry", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses")
                .build();
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
             CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat)) {

            for (SdcSchoolCollectionStudentLightEntity student : entities) {
                String legalFullName = formatFullName(student.getLegalFirstName(), student.getLegalMiddleNames(), student.getLegalLastName());
                String usualFullName = formatFullName(student.getUsualFirstName(), student.getUsualMiddleNames(), student.getUsualLastName());

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
                        student.getNativeAncestryInd(),
                        student.getBandCode(),
                        student.getHomeLanguageSpokenCode(),
                        student.getNumberOfCourses(),
                        student.getSupportBlocks(),
                        student.getOtherCourses()
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

        if (firstName != null && !firstName.isBlank()) {
            fullName.append(firstName);
        }

        if (middleNames != null && !middleNames.isBlank()) {
            if (!fullName.isEmpty()) {
                fullName.append(" ");
            }
            fullName.append(middleNames);
        }

        if (lastName != null && !lastName.isBlank()) {
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
}
