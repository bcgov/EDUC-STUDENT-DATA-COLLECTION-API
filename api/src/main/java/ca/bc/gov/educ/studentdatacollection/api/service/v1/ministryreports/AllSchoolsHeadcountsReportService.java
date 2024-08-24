package ca.bc.gov.educ.studentdatacollection.api.service.v1.ministryreports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolAddress;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySpecialEdAdultHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.SchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.util.LocalDateTimeUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.*;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SpecialEducationHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.flagCountIfNoSchoolFundingGroup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


@Service
@Slf4j
@RequiredArgsConstructor
public class AllSchoolsHeadcountsReportService {
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final CollectionRepository collectionRepository;
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final RestUtils restUtils;
    private static final String COLLECTION_ID = "collectionID";
    private static final String INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB and SEPT collections";

    public DownloadableReportResponse generateAllSchoolsHeadcounts(UUID collectionID) {
        List<SchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllEnrollmentHeadcountsByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }
        var collection = collectionOpt.get();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_YEAR.getCode(), DISTRICT_NUMBER.getCode(), SCHOOL_NUMBER.getCode(), SCHOOL_NAME.getCode(), FACILITY_TYPE.getCode(),
                        SCHOOL_CATEGORY.getCode(), GRADE_RANGE.getCode(), REPORT_DATE.getCode(), KIND_HT_COUNT.getCode(), KIND_FT_COUNT.getCode(), GRADE_01_COUNT.getCode(), GRADE_02_COUNT.getCode(),
                        GRADE_03_COUNT.getCode(),GRADE_04_COUNT.getCode(),GRADE_05_COUNT.getCode(),GRADE_06_COUNT.getCode(),GRADE_07_COUNT.getCode(),GRADE_08_COUNT.getCode(),GRADE_09_COUNT.getCode(),
                        GRADE_10_COUNT.getCode(), GRADE_11_COUNT.getCode(),GRADE_12_COUNT.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SchoolHeadcountResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent()) {
                    List<String> csvRowData = prepareAllSchoolDataForCsv(result, collection, schoolOpt.get());
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SCHOOL_ENROLLMENT_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateIndySchoolsHeadcounts(UUID collectionID) {
        List<IndySchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(IndySchoolEnrolmentHeadcountHeader.SCHOOL.getCode(), GRADE_01.getCode(), GRADE_02.getCode(), GRADE_03.getCode(), GRADE_04.getCode(),
                        GRADE_05.getCode(), GRADE_06.getCode(), GRADE_07.getCode(), GRADE_EU.getCode(), GRADE_08.getCode(), GRADE_09.getCode(), GRADE_10.getCode(),
                        GRADE_11.getCode(),GRADE_12.getCode(),GRADE_SU.getCode(),GRADE_GA.getCode(),GRADE_HS.getCode(),IndySchoolEnrolmentHeadcountHeader.TOTAL.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndySchoolHeadcountResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        List<String> csvRowData = prepareIndySchoolDataForCsv(result, school);
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(INDY_SCHOOL_ENROLLMENT_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generatePhysicalAddressCsv(UUID collectionID) {
        Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
        if(entityOptional.isEmpty()) {
            throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
        }

        List<SdcSchoolCollectionEntity> schoolsInCollection = sdcSchoolCollectionRepository.findAllByCollectionEntityCollectionID(collectionID);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                SchoolAddressHeaders.MINCODE.getCode(), SchoolAddressHeaders.SCHOOL_NAME.getCode(), SchoolAddressHeaders.ADDRESS_LINE1.getCode(),
                SchoolAddressHeaders.ADDRESS_LINE2.getCode(), SchoolAddressHeaders.CITY.getCode(), SchoolAddressHeaders.PROVINCE.getCode(),
                SchoolAddressHeaders.POSTAL.getCode()
                ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionEntity schoolEntity : schoolsInCollection) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(String.valueOf(schoolEntity.getSchoolID()));
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    var schoolAddr = school.getAddresses().stream().filter(address -> address.getAddressTypeCode().equalsIgnoreCase("PHYSICAL")).findFirst();
                    if(schoolAddr.isPresent()) {
                        var address = schoolAddr.get();
                        List<String> csvRowData = prepareSchoolAddressDataForCsv(school, address);
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(SCHOOL_ADDRESS_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateFsaRegistrationCsv(UUID collectionID) {
        Optional<CollectionEntity> entityOptional = collectionRepository.findById(collectionID);
        if(entityOptional.isEmpty()) {
            throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
        }
        CollectionEntity collection = entityOptional.get();
        if(collection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.SEPTEMBER.getTypeCode())) {
            return generateSeptFsaCsv(collection.getCollectionID());
        } else if(collection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.FEBRUARY.getTypeCode())) {
            return generateFebFsaCsv(collection.getCollectionID());
        }
        throw new InvalidPayloadException(createError());
    }

    private DownloadableReportResponse generateFebFsaCsv(UUID collectionID) {
        List<String> grades = Arrays.asList(SchoolGradeCodes.GRADE03.getCode(), SchoolGradeCodes.GRADE06.getCode());
        List<SdcSchoolCollectionStudentEntity> students =
                sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_CollectionEntity_CollectionIDAndEnrolledGradeCodeIn(collectionID, grades);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaFebRegistrationHeader.STUDENT_PEN.getCode(), FsaFebRegistrationHeader.DISTRICT_NUMBER.getCode(), FsaFebRegistrationHeader.SCHOOL_NUMBER.getCode(),
                FsaFebRegistrationHeader.NEXT_YEAR_GRADE.getCode(), FsaFebRegistrationHeader.LOCAL_ID.getCode(), FsaFebRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaFebRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentEntity student : students) {
                String projectedGrade = null;
                if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE03.getCode())) {
                    projectedGrade = SchoolGradeCodes.GRADE04.getCode();
                } else if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE06.getCode())) {
                    projectedGrade = SchoolGradeCodes.GRADE07.getCode();
                }
                List<String> csvRowData = prepareFsaDataForCsv(student, projectedGrade);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(FSA_REGISTRATION_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private DownloadableReportResponse generateSeptFsaCsv(UUID collectionID) {
        List<String> grades = Arrays.asList(SchoolGradeCodes.GRADE04.getCode(), SchoolGradeCodes.GRADE07.getCode());
        List<SdcSchoolCollectionStudentEntity> students =
                sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_CollectionEntity_CollectionIDAndEnrolledGradeCodeIn(collectionID, grades);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaSeptRegistrationHeader.STUDENT_PEN.getCode(), FsaSeptRegistrationHeader.DISTRICT_NUMBER.getCode(), FsaSeptRegistrationHeader.SCHOOL_NUMBER.getCode(),
                FsaSeptRegistrationHeader.ENROLLED_GRADE.getCode(), FsaSeptRegistrationHeader.LOCAL_ID.getCode(), FsaSeptRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaSeptRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentEntity student : students) {
                List<String> csvRowData = prepareFsaDataForCsv(student, student.getEnrolledGradeCode());
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(FSA_REGISTRATION_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateIndySpecialEducationHeadcounts(UUID collectionID) {
        List<IndySpecialEdAdultHeadcountResult> results = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryForIndiesAndOffshoreByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SpecialEducationHeadcountHeader.SCHOOL.getCode(), A.getCode(), B.getCode(), C.getCode(), D.getCode(), E.getCode(),
                        F.getCode(),G.getCode(),H.getCode(),K.getCode(),P.getCode(),Q.getCode(),
                        R.getCode(), SpecialEducationHeadcountHeader.TOTAL.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndySpecialEdAdultHeadcountResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());

                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        List<String> csvRowData = prepareIndyInclusiveEdDataForCsv(result, school);
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(INDY_INCLUSIVE_ED_ENROLLMENT_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generateOffshoreSchoolsHeadcounts(UUID collectionID) {
        List<IndySchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(IndySchoolEnrolmentHeadcountHeader.SCHOOL.getCode(), KIND_HT.getCode(), KIND_FT.getCode(), GRADE_01.getCode(), GRADE_02.getCode(),
                        GRADE_03.getCode(),GRADE_04.getCode(),GRADE_05.getCode(),GRADE_06.getCode(),GRADE_07.getCode(),GRADE_EU.getCode(),
                        GRADE_08.getCode(),GRADE_09.getCode(), GRADE_10.getCode(), GRADE_11.getCode(),GRADE_12.getCode(),
                        GRADE_SU.getCode(),GRADE_GA.getCode(),GRADE_HS.getCode(), IndySchoolEnrolmentHeadcountHeader.TOTAL.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndySchoolHeadcountResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());

                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    if (school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode())) {
                        List<String> csvRowData = prepareOffshoreSchoolDataForCsv(result, school);
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(OFFSHORE_ENROLLMENT_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private List<String> prepareIndyInclusiveEdDataForCsv(IndySpecialEdAdultHeadcountResult result, School school) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                school.getDisplayName(),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdACodes(), result.getAdultsInSpecialEdA()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdBCodes(), result.getAdultsInSpecialEdB()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdCCodes(), result.getAdultsInSpecialEdC()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdDCodes(), result.getAdultsInSpecialEdD()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdECodes(), result.getAdultsInSpecialEdE()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdFCodes(), result.getAdultsInSpecialEdF()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdGCodes(), result.getAdultsInSpecialEdG()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdHCodes(), result.getAdultsInSpecialEdH()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdKCodes(), result.getAdultsInSpecialEdK()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdPCodes(), result.getAdultsInSpecialEdP()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdQCodes(), result.getAdultsInSpecialEdQ()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdRCodes(), result.getAdultsInSpecialEdR()),
                TransformUtil.getTotalHeadcount(result)
        ));
        return csvRowData;
    }

    private List<String> prepareOffshoreSchoolDataForCsv(IndySchoolHeadcountResult result, School school) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                school.getDisplayName(),
                result.getKindHCount(),
                result.getKindFCount(),
                result.getGrade1Count(),
                result.getGrade2Count(),
                result.getGrade3Count(),
                result.getGrade4Count(),
                result.getGrade5Count(),
                result.getGrade6Count(),
                result.getGrade7Count(),
                result.getGradeEUCount(),
                result.getGrade8Count(),
                result.getGrade9Count(),
                result.getGrade10Count(),
                result.getGrade11Count(),
                result.getGrade12Count(),
                result.getGradeSUCount(),
                result.getGradeGACount(),
                result.getGradeHSCount(),
                TransformUtil.getTotalHeadcount(result)
            ));
        return csvRowData;
    }

    private List<String> prepareAllSchoolDataForCsv(SchoolHeadcountResult schoolHeadcountResult, CollectionEntity collection, School school) {
        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                LocalDateTimeUtil.getSchoolYearString(collection),
                school.getMincode().substring(0,3),
                school.getSchoolNumber(),
                school.getDisplayName(),
                school.getFacilityTypeCode(),
                school.getSchoolCategoryCode(),
                TransformUtil.getGradesOfferedString(school),
                collection.getSnapshotDate().toString(),
                schoolHeadcountResult.getKindHCount(),
                schoolHeadcountResult.getKindFCount(),
                schoolHeadcountResult.getGrade1Count(),
                schoolHeadcountResult.getGrade2Count(),
                schoolHeadcountResult.getGrade3Count(),
                schoolHeadcountResult.getGrade4Count(),
                schoolHeadcountResult.getGrade5Count(),
                schoolHeadcountResult.getGrade6Count(),
                schoolHeadcountResult.getGrade7Count(),
                schoolHeadcountResult.getGrade8Count(),
                schoolHeadcountResult.getGrade9Count(),
                schoolHeadcountResult.getGrade10Count(),
                schoolHeadcountResult.getGrade11Count(),
                schoolHeadcountResult.getGrade12Count()
        ));
        return csvRowData;
    }

    private List<String> prepareIndySchoolDataForCsv(IndySchoolHeadcountResult indySchoolHeadcountResult, School school) {
        var schoolFundingGroupGrades = school.getSchoolFundingGroups().stream().map(IndependentSchoolFundingGroup::getSchoolGradeCode).toList();
        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                school.getDisplayName(),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.KINDHALF.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getKindHCount()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.KINDFULL.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getKindFCount()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE01.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade1Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE02.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade2Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE03.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade3Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE04.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade4Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE05.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade5Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE06.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade6Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE07.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade7Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.ELEMUNGR.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeEUCount()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE08.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade8Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE09.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade9Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE10.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade10Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE11.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade11Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADE12.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGrade12Count()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeSUCount()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.GRADUATED_ADULT.getTypeCode(), schoolFundingGroupGrades,  indySchoolHeadcountResult.getGradeGACount()),
                flagCountIfNoSchoolFundingGroup(SchoolGradeCodes.HOMESCHOOL.getTypeCode(), schoolFundingGroupGrades, indySchoolHeadcountResult.getGradeHSCount()),
                TransformUtil.getTotalHeadcount(indySchoolHeadcountResult)
        ));

        return csvRowData;
    }

    private List<String> prepareFsaDataForCsv(SdcSchoolCollectionStudentEntity student, String studentGrade) {
        var schoolOpt = restUtils.getAllSchoolBySchoolID(String.valueOf(student.getSdcSchoolCollection().getSchoolID()));
        List<String> csvRowData = new ArrayList<>();
        if(schoolOpt.isPresent()) {
            var school = schoolOpt.get();

            csvRowData.addAll(Arrays.asList(
                    student.getAssignedPen(),
                    school.getMincode().substring(0,3),
                    school.getSchoolNumber(),
                    studentGrade,
                    student.getLocalID(),
                    student.getLegalFirstName(),
                    student.getLegalLastName()
            ));
        }
        return csvRowData;
    }

    private List<String> prepareSchoolAddressDataForCsv(School school, SchoolAddress address) {
        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                school.getMincode(),
                school.getDisplayName(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getProvinceCode(),
                address.getPostal()
        ));
        return csvRowData;
    }

    private ApiError createError() {
        return ApiError.builder().timestamp(LocalDateTime.now()).message(INVALID_COLLECTION_TYPE).status(BAD_REQUEST).build();
    }
}
