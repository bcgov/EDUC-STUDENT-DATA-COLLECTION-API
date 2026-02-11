package ca.bc.gov.educ.studentdatacollection.api.service.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.reports.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@Service
@Slf4j
@RequiredArgsConstructor
public class AllReportsService {
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final GradeEnrollmentHeadcountReportService gradeEnrollmentHeadcountReportService;
    private final CareerProgramHeadcountReportService careerProgramHeadcountReportService;
    private final FrenchProgramHeadcountReportService frenchProgramHeadcountReportService;
    private final IndigenousHeadcountReportService indigenousHeadcountReportService;
    private final EllHeadcountReportService ellHeadcountReportService;
    private final SpecialEdHeadcountReportService specialEdHeadcountReportService;
    private final AllStudentLightCollectionGenerateCsvService allStudentLightCollectionGenerateCsvService;
    private final BandOfResidenceHeadcountReportService bandOfResidenceHeadcountReportService;
    private final ZeroFTEHeadCountReportService zeroFTEHeadCountReportService;
    private final FrenchPerSchoolHeadcountReportService frenchPerSchoolHeadcountReportService;
    private final IndigenousPerSchoolHeadcountReportService indigenousPerSchoolHeadcountReportService;
    private final EllHeadcountPerSchoolReportService ellHeadcountPerSchoolReportService;
    private final SpecialEdHeadcountPerSchoolReportService specialEdHeadcountPerSchoolReportService;
    private final SpecialEdCategoryHeadcountPerSchoolReportService inclusiveEdCategoryHeadcountPerSchoolReportService;
    private final BandOfResidenceHeadcountPerSchoolReportService bandOfResidenceHeadcountPerSchoolReportService;
    private final GradeEnrollmentHeadcountPerSchoolReportService gradeEnrollmentHeadcountPerSchoolReportService;
    private final CareerProgramHeadcountPerSchoolReportService careerProgramHeadcountPerSchoolReportService;
    private final RefugeeHeadcountPerSchoolReportService refugeeHeadcountPerSchoolReportService;
    private final RestUtils restUtils;

    @PersistenceContext
    private EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int ENTITY_MANAGER_CLEAR_INTERVAL = 50;

    @Async
    public void generateAllDistrictReportsOnDisk(UUID sdcDistrictCollectionID) {
        var districtCollection = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
        var schoolCollections = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(sdcDistrictCollectionID);
        var district = restUtils.getDistrictByDistrictID(districtCollection.get().getDistrictID().toString());

        schoolCollections.forEach(schoolCollection -> {
           var school = restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString());
           new File("./DistrictFiles/" + school.get().getMincode()).mkdirs();

           var report1 = gradeEnrollmentHeadcountReportService.generateSchoolGradeEnrollmentHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_GradeEnrollmentHeadcount.pdf", report1.getDocumentData());

           var report2 = careerProgramHeadcountReportService.generateSchoolCareerProgramHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_CareerProgramHeadcount.pdf", report2.getDocumentData());

           var report3 = frenchProgramHeadcountReportService.generateSchoolFrenchProgramHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_FrenchProgramHeadcount.pdf", report3.getDocumentData());

           var report4 = indigenousHeadcountReportService.generateSchoolIndigenousHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_IndigenousHeadcount.pdf", report4.getDocumentData());

           var report5 = ellHeadcountReportService.generateSchoolEllHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_EllHeadcount.pdf", report5.getDocumentData());

           var report6 = specialEdHeadcountReportService.generateSchoolSpecialEdHeadcountReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_InclusiveEducationHeadcount.pdf", report6.getDocumentData());

           var report7 = allStudentLightCollectionGenerateCsvService.generateFromSdcSchoolCollectionID(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_AllStudents.csv", report7.getDocumentData());

           var report8 = bandOfResidenceHeadcountReportService.generateSchoolBandOfResidenceReport(schoolCollection.getSdcSchoolCollectionID());
           decodeAndWritePDFToDisk("./DistrictFiles/" + school.get().getMincode() + "/" + school.get().getMincode() + "_BandOfResidenceHeadcount.pdf", report8.getDocumentData());

           log.info("Completed school files for mincode " + school.get().getMincode());
        });
        log.info("Generating district files...");

        var districtReport1 = gradeEnrollmentHeadcountReportService.generateDistrictGradeEnrollmentHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_GradeEnrollmentHeadcount.pdf", districtReport1.getDocumentData());

        var districtReport2 = careerProgramHeadcountReportService.generateDistrictCareerProgramHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_CareerProgramHeadcount.pdf", districtReport2.getDocumentData());

        var districtReport3 = frenchProgramHeadcountReportService.generateDistrictFrenchProgramHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_FrenchProgramHeadcount.pdf", districtReport3.getDocumentData());

        var districtReport4 = indigenousHeadcountReportService.generateDistrictIndigenousHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_IndigenousHeadcount.pdf", districtReport4.getDocumentData());

        var districtReport5 = ellHeadcountReportService.generateDistrictEllHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_EllHeadcount.pdf", districtReport5.getDocumentData());

        var districtReport6 = specialEdHeadcountReportService.generateDistrictSpecialEdHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_InclusiveEducationHeadcount.pdf", districtReport6.getDocumentData());

        var districtReport7 = allStudentLightCollectionGenerateCsvService.generateFromSdcDistrictCollectionID(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_AllStudents.csv", districtReport7.getDocumentData());

        var districtReport8 = bandOfResidenceHeadcountReportService.generateDistrictBandOfResidenceReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_BandOfResidenceHeadcount.pdf", districtReport8.getDocumentData());

        var districtReport9 = zeroFTEHeadCountReportService.generateZeroFTEHeadcountReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_ZeroFTEHeadcount.pdf", districtReport9.getDocumentData());

        var districtReport10 = frenchPerSchoolHeadcountReportService.generatePerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_FrenchPerSchoolHeadcount.pdf", districtReport10.getDocumentData());

        var districtReport11 = indigenousPerSchoolHeadcountReportService.generateIndigenousHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_IndigenousPerSchoolHeadcount.pdf", districtReport11.getDocumentData());

        var districtReport12 = ellHeadcountPerSchoolReportService.generateEllHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_EllPerSchoolHeadcount.pdf", districtReport12.getDocumentData());

        var districtReport13 = specialEdHeadcountPerSchoolReportService.generateSpecialEdHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_InclusiveEducationPerSchoolHeadcount.pdf", districtReport13.getDocumentData());

        var districtReport14 = inclusiveEdCategoryHeadcountPerSchoolReportService.generateInclusiveEdCategoryHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_InclusiveEducationCategoryPerSchoolHeadcount.pdf", districtReport14.getDocumentData());

        var districtReport15 = bandOfResidenceHeadcountPerSchoolReportService.generateBandOfResidenceHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_BandOfResidencePerSchoolHeadcount.pdf", districtReport15.getDocumentData());

        var districtReport16 = gradeEnrollmentHeadcountPerSchoolReportService.generatePerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_GradeEnrollmentPerSchoolHeadcount.pdf", districtReport16.getDocumentData());

        var districtReport17 = careerProgramHeadcountPerSchoolReportService.generateCareerProgramHeadcountPerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_CareerProgramPerSchoolHeadcount.pdf", districtReport17.getDocumentData());

        var districtReport18 = refugeeHeadcountPerSchoolReportService.generateRefugeePerSchoolReport(sdcDistrictCollectionID);
        decodeAndWritePDFToDisk("./DistrictFiles/" + district.get().getDistrictNumber() + "_RefugeePerSchoolHeadcount.pdf", districtReport18.getDocumentData());

        log.info("District file generation complete!");
    }

    private void decodeAndWritePDFToDisk(String filename, String bytes) {
        File file = new File(filename);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            byte[] decoder = Base64.getDecoder().decode(bytes);
            fos.write(decoder);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FunctionalInterface
    private interface ReportGenerator {
        DownloadableReportResponse generate();
    }

    @Transactional(readOnly = true)
    public void generateAllDistrictReportsStreamChunked(UUID sdcDistrictCollectionID, HttpServletResponse response) throws IOException {
        var districtCollection = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID);
        if (districtCollection.isEmpty()) {
            log.error("District collection not found for ID: {}", sdcDistrictCollectionID);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "District collection not found");
            return;
        }

        var district = restUtils.getDistrictByDistrictID(districtCollection.get().getDistrictID().toString());
        if (district.isEmpty()) {
            log.error("District not found for district collection: {}", sdcDistrictCollectionID);
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "District not found");
            return;
        }

        String districtNumber = district.get().getDistrictNumber();

        response.setContentType("application/x-ndjson;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("X-Accel-Buffering", "no");

        AtomicInteger schoolCount = new AtomicInteger(0);
        AtomicInteger reportCount = new AtomicInteger(0);
        AtomicBoolean clientDisconnected = new AtomicBoolean(false);

        log.debug("Starting chunked stream generation for district collection: {}", sdcDistrictCollectionID);

        try (PrintWriter writer = response.getWriter();
             Stream<ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity> schoolCollectionStream =
                     sdcSchoolCollectionRepository.streamAllBySdcDistrictCollectionID(sdcDistrictCollectionID)) {

            log.debug("Stream initialized, sending start message");

            sendJsonMessage(writer, Map.of(
                "type", "start",
                "districtNumber", districtNumber,
                "timestamp", LocalDateTime.now().toString()
            ));

            log.debug("Start message sent, beginning school processing");

            schoolCollectionStream
                    .takeWhile(sc -> !clientDisconnected.get())
                    .forEach(schoolCollection -> {
                        try {
                            var school = restUtils.getSchoolBySchoolID(schoolCollection.getSchoolID().toString());
                            if (school.isEmpty()) {
                                sendJsonMessage(writer, Map.of(
                                    "type", "warning",
                                    "message", "School not found for ID " + schoolCollection.getSchoolID()
                                ));
                                return;
                            }

                            String mincode = school.get().getMincode();

                            streamReport(writer, mincode + "/", mincode + "_GradeEnrollmentHeadcount.pdf",
                                    () -> gradeEnrollmentHeadcountReportService.generateSchoolGradeEnrollmentHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_CareerProgramHeadcount.pdf",
                                    () -> careerProgramHeadcountReportService.generateSchoolCareerProgramHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_FrenchProgramHeadcount.pdf",
                                    () -> frenchProgramHeadcountReportService.generateSchoolFrenchProgramHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_IndigenousHeadcount.pdf",
                                    () -> indigenousHeadcountReportService.generateSchoolIndigenousHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_EllHeadcount.pdf",
                                    () -> ellHeadcountReportService.generateSchoolEllHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_InclusiveEducationHeadcount.pdf",
                                    () -> specialEdHeadcountReportService.generateSchoolSpecialEdHeadcountReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_AllStudents.csv",
                                    () -> allStudentLightCollectionGenerateCsvService.generateFromSdcSchoolCollectionID(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            streamReport(writer, mincode + "/", mincode + "_BandOfResidenceHeadcount.pdf",
                                    () -> bandOfResidenceHeadcountReportService.generateSchoolBandOfResidenceReport(schoolCollection.getSdcSchoolCollectionID()));
                            reportCount.incrementAndGet();

                            int count = schoolCount.incrementAndGet();

                            sendJsonMessage(writer, Map.of(
                                "type", "progress",
                                "message", "Completed school " + mincode,
                                "schoolsProcessed", count,
                                "reportsGenerated", reportCount.get()
                            ));

                            if (count % ENTITY_MANAGER_CLEAR_INTERVAL == 0) {
                                entityManager.clear();
                                log.debug("Cleared entity manager at school count: {}", count);
                            }

                        } catch (IOException e) {
                            log.debug("Client disconnected during report generation at school {}. Stopping stream.", schoolCount.get());
                            log.debug("IOException caught: {}", e.getMessage());
                            clientDisconnected.set(true);
                        } catch (Exception e) {
                            log.error("Error generating reports for school collection {}: {}",
                                    schoolCollection.getSdcSchoolCollectionID(), e.getMessage(), e);
                            try {
                                sendJsonMessage(writer, Map.of(
                                    "type", "error",
                                    "message", "Error processing school: " + e.getMessage(),
                                    "fatal", false
                                ));
                            } catch (IOException ioException) {
                                log.debug("Client disconnected during error reporting at school {}", schoolCount.get());
                                log.debug("IOException during error reporting: {}", ioException.getMessage());
                                clientDisconnected.set(true);
                            }
                        }
                    });

            if (clientDisconnected.get()) {
                log.debug("Chunked streaming stopped due to client disconnect after {} schools", schoolCount.get());
                log.debug("Client disconnect detected, exiting early");
                return;
            }

            log.debug("School processing complete, generating district-level reports");

            sendJsonMessage(writer, Map.of(
                "type", "progress",
                "message", "Generating district-level reports..."
            ));

            // Generate district-level reports
            streamReport(writer, "", districtNumber + "_GradeEnrollmentHeadcount.pdf",
                    () -> gradeEnrollmentHeadcountReportService.generateDistrictGradeEnrollmentHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_CareerProgramHeadcount.pdf",
                    () -> careerProgramHeadcountReportService.generateDistrictCareerProgramHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_FrenchProgramHeadcount.pdf",
                    () -> frenchProgramHeadcountReportService.generateDistrictFrenchProgramHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_IndigenousHeadcount.pdf",
                    () -> indigenousHeadcountReportService.generateDistrictIndigenousHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_EllHeadcount.pdf",
                    () -> ellHeadcountReportService.generateDistrictEllHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_InclusiveEducationHeadcount.pdf",
                    () -> specialEdHeadcountReportService.generateDistrictSpecialEdHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_AllStudents.csv",
                    () -> allStudentLightCollectionGenerateCsvService.generateFromSdcDistrictCollectionID(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_BandOfResidenceHeadcount.pdf",
                    () -> bandOfResidenceHeadcountReportService.generateDistrictBandOfResidenceReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_ZeroFTEHeadcount.pdf",
                    () -> zeroFTEHeadCountReportService.generateZeroFTEHeadcountReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_FrenchPerSchoolHeadcount.pdf",
                    () -> frenchPerSchoolHeadcountReportService.generatePerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_IndigenousPerSchoolHeadcount.pdf",
                    () -> indigenousPerSchoolHeadcountReportService.generateIndigenousHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_EllPerSchoolHeadcount.pdf",
                    () -> ellHeadcountPerSchoolReportService.generateEllHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_InclusiveEducationPerSchoolHeadcount.pdf",
                    () -> specialEdHeadcountPerSchoolReportService.generateSpecialEdHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_InclusiveEducationCategoryPerSchoolHeadcount.pdf",
                    () -> inclusiveEdCategoryHeadcountPerSchoolReportService.generateInclusiveEdCategoryHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_BandOfResidencePerSchoolHeadcount.pdf",
                    () -> bandOfResidenceHeadcountPerSchoolReportService.generateBandOfResidenceHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_GradeEnrollmentPerSchoolHeadcount.pdf",
                    () -> gradeEnrollmentHeadcountPerSchoolReportService.generatePerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_CareerProgramPerSchoolHeadcount.pdf",
                    () -> careerProgramHeadcountPerSchoolReportService.generateCareerProgramHeadcountPerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            streamReport(writer, "", districtNumber + "_RefugeePerSchoolHeadcount.pdf",
                    () -> refugeeHeadcountPerSchoolReportService.generateRefugeePerSchoolReport(sdcDistrictCollectionID));
            reportCount.incrementAndGet();

            // Send completion message
            sendJsonMessage(writer, Map.of(
                "type", "complete",
                "schoolsProcessed", schoolCount.get(),
                "reportsGenerated", reportCount.get(),
                "timestamp", LocalDateTime.now().toString()
            ));

            log.debug("Successfully completed chunked stream generation for district collection: {} ({} schools, {} reports)",
                    sdcDistrictCollectionID, schoolCount.get(), reportCount.get());

        } catch (IOException e) {
            log.error("I/O error during chunked streaming: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during chunked streaming: {}", e.getMessage(), e);
            try {
                sendJsonMessage(response.getWriter(), Map.of(
                    "type", "error",
                    "message", "Fatal error: " + e.getMessage(),
                    "fatal", true
                ));
            } catch (IOException ioException) {
                log.error("Failed to send error response: {}", ioException.getMessage());
            }
        }
    }

    private void streamReport(PrintWriter writer, String directory, String filename, ReportGenerator generator) throws IOException {
        try {
            var report = generator.generate();
            sendJsonMessage(writer, Map.of(
                "type", "file",
                "path", directory + filename,
                "filename", filename,
                "data", report.getDocumentData()
            ));
            log.trace("Streamed {} to client", directory + filename);
        } catch (Exception e) {
            log.error("Error streaming report {}: {}", filename, e.getMessage(), e);
            sendJsonMessage(writer, Map.of(
                "type", "file",
                "path", directory + filename + ".error.txt",
                "filename", filename + ".error.txt",
                "data", Base64.getEncoder().encodeToString(("Error generating report: " + e.getMessage()).getBytes())
            ));
        }
    }

    private void sendJsonMessage(PrintWriter writer, Map<String, Object> message) throws IOException {
        String json = objectMapper.writeValueAsString(message);
        writer.write(json);
        writer.write("\n");
        writer.flush();

        if (writer.checkError()) {
            log.debug("Client disconnect detected via writer.checkError()");
            throw new IOException("Client disconnected");
        }
    }
}
