package ca.bc.gov.educ.studentdatacollection.api.service.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.reports.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;
import java.util.UUID;


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
}
