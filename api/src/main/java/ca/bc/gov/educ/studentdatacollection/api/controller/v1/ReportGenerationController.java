package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ReportTypeCode;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.ReportGenerationEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.reports.*;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationController implements ReportGenerationEndpoint {

    private final GradeEnrollmentHeadcountReportService gradeEnrollmentHeadcountReportService;
    private final CareerProgramHeadcountReportService careerProgramHeadcountReportService;
    private final FrenchProgramHeadcountReportService frenchProgramHeadcountReportService;
    private final IndigenousHeadcountReportService indigenousHeadcountReportService;
    private final EllHeadcountReportService ellHeadcountReportService;
    private final SpecialEdHeadcountReportService specialEdHeadcountReportService;
    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;

    @Override
    public DownloadableReportResponse generateSDCReport(UUID collectionID, String reportTypeCode) {
        Optional<ReportTypeCode> code = ReportTypeCode.findByValue(reportTypeCode);

        if(code.isEmpty()){
            ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid report type code.").status(BAD_REQUEST).build();
            throw new InvalidPayloadException(error);
        }

        switch(code.get()){
            case GRADE_ENROLLMENT_HEADCOUNT:
                return gradeEnrollmentHeadcountReportService.generateGradeEnrollmentHeadcountReport(collectionID);
            case CAREER_HEADCOUNT:
                return careerProgramHeadcountReportService.generateCareerProgramHeadcountReport(collectionID);
            case FRENCH_HEADCOUNT:
                return frenchProgramHeadcountReportService.generateFrenchProgramHeadcountReport(collectionID);
            case INDIGENOUS_HEADCOUNT:
                return indigenousHeadcountReportService.generateIndigenousHeadcountReport(collectionID);
            case ELL_HEADCOUNT:
                return ellHeadcountReportService.generateEllHeadcountReport(collectionID);
            case SPECIAL_EDUCATION_HEADCOUNT:
                return specialEdHeadcountReportService.generateSpecialEdHeadcountReport(collectionID);
            case ALL_STUDENT_CSV:
                List<SdcSchoolCollectionStudentLightEntity> entities = sdcSchoolCollectionStudentSearchService.findAllStudentsLightSynchronous(collectionID);
                log.info("Start create CSV");
                try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                     CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(baos), CSVFormat.DEFAULT
                             .withHeader("P.E.N.", "Legal Name", "Usual Name", "Birth Date", "Gender", "Postal Code", "Local ID", "Grade", "F.T.E.", "Adult", "Graduate",
                                     "Native Ancestry", "Band Code", "Home Language", "# Courses", "# Support Blocks", "# Other Courses"))) {

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

                    log.info("Finish create CSV");

                    var downloadableReport = new DownloadableReportResponse();
                    downloadableReport.setReportType(reportTypeCode);
                    downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(baos.toByteArray()));

                    return downloadableReport;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to generate CSV", e);
                }

            default:
                return new DownloadableReportResponse();
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
}
