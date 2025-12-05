package ca.bc.gov.educ.studentdatacollection.api.service.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.dto.sdc.SdcSchoolCollectionIdSchoolId;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.IndependentSchoolFundingGroupSnapshotService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.ValidationRulesService;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.HomeLanguageSpokenCode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.DownloadableReportResponse;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.SpedFundingReportTotals;
import ca.bc.gov.educ.studentdatacollection.api.util.LocalDateTimeUtil;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.QuoteMode;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.DISTRICT_NUMBER;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.SCHOOL_NAME;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private final SdcDuplicateRepository sdcDuplicateRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final SdcSchoolCollectionStudentFsaReportRepository sdcSchoolCollectionStudentFsaReportRepository;
    private final CollectionRepository collectionRepository;
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionLightRepository sdcSchoolCollectionLightRepository;
    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final IndependentSchoolFundingGroupSnapshotService independentSchoolFundingGroupSnapshotService;
    private final RestUtils restUtils;
    private final ValidationRulesService validationService;
    private static final String COLLECTION_ID = "collectionID";
    private static final String SCHOOL_ID = "schoolID";
    private static final String DISTRICT_ID = "districtID";
    private static final String INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB and SEPT collections";
    private static final String HEADCOUNTS_INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB and MAY collections";
    private static final String REFUGEE_HEADCOUNTS_INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB collection";
    private static final String REPORT_DATE_FORMAT = "yyyyMMdd";

    // Independent School Funding Report - Standard Student report AND Independent School Funding Report - Online Learning report AND Independent School Funding Report - Non Graduated Adult report
    public DownloadableReportResponse generateIndyFundingReport(UUID collectionID, boolean isOnlineLearning, boolean isFundedReport) {
        List<IndyFundingResult> results = sdcSchoolCollectionStudentRepository.getIndyFundingHeadcountsByCollectionId(collectionID);

        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }
        String[] headers = isOnlineLearning ? IndyFundingReportHeader.getAllValuesAndRoundUpAsStringArray() : IndyFundingReportHeader.getAllValuesAsStringArray();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndyFundingResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();

                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        var districtOpt = restUtils.getDistrictByDistrictID(school.getDistrictId());

                        District district = null;
                        if (districtOpt.isPresent()) {
                            district = districtOpt.get();
                        }

                        Optional<IndependentAuthority> authorityOpt = Optional.empty();
                        if (school.getIndependentAuthorityId() != null) {
                            authorityOpt = restUtils.getAuthorityByAuthorityID(school.getIndependentAuthorityId());
                        }

                        IndependentAuthority authority = null;
                        if (authorityOpt.isPresent()) {
                            authority = authorityOpt.get();
                        }

                        // If it's online learning, only include schools with online facility types
                        List<String> csvRowData = null;
                        if (!isOnlineLearning || FacilityTypeCodes.getOnlineFacilityTypeCodes().contains(school.getFacilityTypeCode())) {
                            if(isFundedReport) {
                                csvRowData = prepareIndyFundedDataForCsv(result, school, district, authority, collectionOpt.get());
                            }else{
                                csvRowData = prepareIndyAllDataForCsv(result, school, district, authority, collectionOpt.get(), isOnlineLearning);
                            }
                        }

                        if (csvRowData != null) {
                            csvPrinter.printRecord(csvRowData);
                        }
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            if (isOnlineLearning) {
                downloadableReport.setReportType(ONLINE_INDY_FUNDING_REPORT.getCode());
            }else if (isFundedReport) {
                downloadableReport.setReportType(INDY_FUNDING_REPORT_FUNDED.getCode());
            }else{
                downloadableReport.setReportType(INDY_FUNDING_REPORT_ALL.getCode());
            }
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    // Independent School Funding Report - Standard Student report AND Independent School Funding Report - Online Learning report AND Independent School Funding Report - Non Graduated Adult report
    public DownloadableReportResponse generateIndyFundingGraduateReport(UUID collectionID) {
        List<IndyFundingGraduatedResult> results = sdcSchoolCollectionStudentRepository.getIndyFundingHeadcountsNonGraduatedAdultByCollectionId(collectionID);

        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(IndyFundingGraduatedReportHeader.getAllValuesAsStringArray())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndyFundingGraduatedResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();

                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        var districtOpt = restUtils.getDistrictByDistrictID(school.getDistrictId());

                        District district = null;
                        if (districtOpt.isPresent()) {
                            district = districtOpt.get();
                        }

                        Optional<IndependentAuthority> authorityOpt = Optional.empty();
                        if (school.getIndependentAuthorityId() != null) {
                            authorityOpt = restUtils.getAuthorityByAuthorityID(school.getIndependentAuthorityId());
                        }

                        IndependentAuthority authority = null;
                        if (authorityOpt.isPresent()) {
                            authority = authorityOpt.get();
                        }

                        // If it's online learning, only include schools with online facility types
                        var csvRowData = prepareIndyAllDataForGraduatedCsv(result, school, district, authority, collectionOpt.get());

                        if (csvRowData != null) {
                            csvPrinter.printRecord(csvRowData);
                        }
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();

            downloadableReport.setReportType(NON_GRADUATED_ADULT_INDY_FUNDING_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    public DownloadableReportResponse generatePostedDuplicatesReport(UUID collectionID) {
        List<SdcDuplicateEntity> results = sdcDuplicateRepository.findAllByCollectionID(collectionID);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(DuplicatesListHeader.getAllValuesAsStringArray())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcDuplicateEntity result : results) {
                for (SdcDuplicateStudentEntity stud : result.getSdcDuplicateStudentEntities()) {
                    var schoolOpt = restUtils.getSchoolBySchoolID(stud.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionEntity().getSchoolID().toString());

                    if(schoolOpt.isPresent()) {
                        var school = schoolOpt.get();
                        District district = null;
                        if(stud.getSdcSchoolCollectionStudentEntity().getSdcSchoolCollectionEntity().getSdcDistrictCollectionID() != null){
                            district = restUtils.getDistrictByDistrictID(school.getDistrictId()).orElseThrow(() -> new EntityNotFoundException(District.class, DISTRICT_ID, school.getDistrictId()));
                        }

                        List<String> csvRowData = prepareStudentDupeForCsv(stud, school, district, result.getDuplicateTypeCode());
                        csvPrinter.printRecord(csvRowData);
                    }
                }
                csvPrinter.printRecord("");
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(POSTED_DUPLICATES.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    // School Enrolment Headcounts report
    public DownloadableReportResponse generateAllSchoolsHeadcounts(UUID collectionID) {
        List<SchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllEnrollmentHeadcountsByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }
        var collection = collectionOpt.get();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL_YEAR.getCode(), DISTRICT_NUMBER.getCode(), SchoolEnrolmentHeader.SCHOOL_NUMBER.getCode(), SCHOOL_NAME.getCode(), FACILITY_TYPE.getCode(),
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

    // Independent School Enrolment Headcounts report
    public DownloadableReportResponse generateIndySchoolsHeadcounts(UUID collectionID) {
        List<IndySchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);
        CollectionEntity collectionEntity = collectionRepository.findById(collectionID).orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString()));

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL.getCode(), REPORT_DATE.getCode(), KIND_HT.getCode(), KIND_FT.getCode(),GRADE_01.getCode(), GRADE_02.getCode(), GRADE_03.getCode(), GRADE_04.getCode(),
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
                        List<String> csvRowData = prepareIndySchoolDataForCsv(result, school, collectionEntity);
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
                    if(!school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode()) && !school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.YUKON.getCode())) {
                        var schoolAddr = school.getAddresses().stream()
                                .filter(address -> address.getAddressTypeCode().equalsIgnoreCase("PHYSICAL"))
                                .findFirst()
                                .or(() -> school.getAddresses().stream().filter(address -> address.getAddressTypeCode().equalsIgnoreCase("MAILING")).findFirst());
                        if(schoolAddr.isPresent()) {
                            var address = schoolAddr.get();
                            List<String> csvRowData = prepareSchoolAddressDataForCsv(school, address);
                            csvPrinter.printRecord(csvRowData);
                        }
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

    // FSA Registration Report
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
        throw new InvalidPayloadException(createError(INVALID_COLLECTION_TYPE));
    }

    private DownloadableReportResponse generateFebFsaCsv(UUID collectionID) {
        List<String> grades = Arrays.asList(SchoolGradeCodes.GRADE03.getCode(), SchoolGradeCodes.GRADE06.getCode());
        List<SdcSchoolCollectionIdSchoolId> sdcSchoolCollectionIdSchoolIds = sdcSchoolCollectionLightRepository.findSdcSchoolCollectionIdSchoolIdByCollectionId(collectionID);
        List<UUID> sdcSchoolCollectionIDs = sdcSchoolCollectionIdSchoolIds.stream()
                .map(SdcSchoolCollectionIdSchoolId::getSdcSchoolCollectionID)
                .toList();
        Map<UUID, UUID> collectionToSchoolIdMap = sdcSchoolCollectionIdSchoolIds.stream()
                .collect(Collectors.toMap(SdcSchoolCollectionIdSchoolId::getSdcSchoolCollectionID, SdcSchoolCollectionIdSchoolId::getSchoolID));
        List<SdcSchoolCollectionStudentFsaReportEntity> students =
                sdcSchoolCollectionStudentFsaReportRepository.findAllBySdcSchoolCollectionIDsAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(
                        sdcSchoolCollectionIDs, grades, SdcSchoolStudentStatus.DELETED.getCode());

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaFebRegistrationHeader.MINCODE.getCode(), FsaFebRegistrationHeader.STUDENT_PEN.getCode(),
                FsaFebRegistrationHeader.NEXT_YEAR_GRADE.getCode(), FsaFebRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaFebRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentFsaReportEntity student : students) {
                UUID sdcSchoolCollectionID = student.getSdcSchoolCollectionID();
                UUID schoolID = collectionToSchoolIdMap.get(sdcSchoolCollectionID);
                var schoolOpt = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));
                if(schoolOpt.isPresent() && !schoolOpt.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode()) &&
                                !schoolOpt.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.YUKON.getCode())) {
                    List<String> csvRowData = prepareFsaDataForCsv(student, TransformUtil.getProjectedGrade(student), schoolOpt.get());
                    csvPrinter.printRecord(csvRowData);
                }
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
        List<SdcSchoolCollectionIdSchoolId> sdcSchoolCollectionIdSchoolId = sdcSchoolCollectionLightRepository.findSdcSchoolCollectionIdSchoolIdByCollectionId(collectionID);
        List<UUID> sdcSchoolCollectionIDs = sdcSchoolCollectionIdSchoolId.stream()
                .map(SdcSchoolCollectionIdSchoolId::getSdcSchoolCollectionID)
                .toList();
        Map<UUID, UUID> collectionToSchoolIdMap = sdcSchoolCollectionIdSchoolId.stream()
                .collect(Collectors.toMap(SdcSchoolCollectionIdSchoolId::getSdcSchoolCollectionID, SdcSchoolCollectionIdSchoolId::getSchoolID));
        List<SdcSchoolCollectionStudentFsaReportEntity> students =
                sdcSchoolCollectionStudentFsaReportRepository.findAllBySdcSchoolCollectionIDsAndEnrolledGradeCodeInAndSdcSchoolCollectionStudentStatusCodeIsNot(
                        sdcSchoolCollectionIDs, grades, SdcSchoolStudentStatus.DELETED.getCode());

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaSeptRegistrationHeader.MINCODE.getCode(), FsaSeptRegistrationHeader.STUDENT_PEN.getCode(),
                FsaSeptRegistrationHeader.ENROLLED_GRADE.getCode(), FsaSeptRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaSeptRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentFsaReportEntity student : students) {
                UUID sdcSchoolCollectionID = student.getSdcSchoolCollectionID();
                UUID schoolID = collectionToSchoolIdMap.get(sdcSchoolCollectionID);
                var schoolOpt = restUtils.getSchoolBySchoolID(String.valueOf(schoolID));
                if(schoolOpt.isPresent() && !schoolOpt.get().getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.YUKON.getCode())) {
                    List<String> csvRowData = prepareFsaDataForCsv(student, student.getEnrolledGradeCode(), schoolOpt.get());
                    csvPrinter.printRecord(csvRowData);
                }
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

    // Independent School Inclusive Education Headcounts report
    public DownloadableReportResponse generateIndySpecialEducationHeadcounts(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        List<IndySpecialEdAdultHeadcountResult> results;

        if(Objects.equals(collectionOpt.get().getCollectionTypeCode(), CollectionTypeCodes.FEBRUARY.getTypeCode())){
            results = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryForIndiesAndOffshoreFebruaryByCollectionId(collectionID);
        } else {
            results = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryForIndiesAndOffshoreByCollectionId(collectionID);
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        IndySpecialEducationHeadcountHeader.AUTHORITY_NUMBER.getCode(), IndySpecialEducationHeadcountHeader.AUTHORITY_NAME.getCode(),
                        IndySpecialEducationHeadcountHeader.MIN_CODE.getCode(),
                        IndySpecialEducationHeadcountHeader.SCHOOL.getCode(),
                        IndySpecialEducationHeadcountHeader.REPORT_DATE.getCode(),
                        IndySpecialEducationHeadcountHeader.LEVEL_1.getCode(), IndySpecialEducationHeadcountHeader.A.getCode(), IndySpecialEducationHeadcountHeader.B.getCode(),
                        IndySpecialEducationHeadcountHeader.LEVEL_2.getCode(), IndySpecialEducationHeadcountHeader.C.getCode(), IndySpecialEducationHeadcountHeader.D.getCode(), IndySpecialEducationHeadcountHeader.E.getCode(), IndySpecialEducationHeadcountHeader.F.getCode(), IndySpecialEducationHeadcountHeader.G.getCode(),
                        IndySpecialEducationHeadcountHeader.LEVEL_3.getCode(), IndySpecialEducationHeadcountHeader.H.getCode(),
                        IndySpecialEducationHeadcountHeader.LEVEL_OTHER.getCode(), IndySpecialEducationHeadcountHeader.K.getCode(), IndySpecialEducationHeadcountHeader.P.getCode(), IndySpecialEducationHeadcountHeader.Q.getCode(), IndySpecialEducationHeadcountHeader.R.getCode(),
                        IndySpecialEducationHeadcountHeader.TOTAL.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndySpecialEdAdultHeadcountResult result : results) {
                var schoolOpt = restUtils.getAllSchoolBySchoolID(result.getSchoolID());

                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    Optional<IndependentAuthority> authorityOpt = Optional.empty();
                    if (school.getIndependentAuthorityId() != null) {
                        authorityOpt = restUtils.getAuthorityByAuthorityID(school.getIndependentAuthorityId());
                    }

                    IndependentAuthority authority = null;
                    if (authorityOpt.isPresent()) {
                        authority = authorityOpt.get();
                    }
                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        List<String> csvRowData = prepareIndyInclusiveEdDataForCsv(result, school, authority, collectionOpt.get());
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

    // Independent School Inclusive Education Funding Headcounts report
    public DownloadableReportResponse generateIndySpecialEducationFundingHeadcounts(UUID collectionID) {
        List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsFebruaryByCollectionId(collectionID);

        var collection = collectionRepository.findById(collectionID).orElseThrow(() ->
                new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString()));

        var mappedSeptData = getLastSeptCollectionSchoolMap(collection);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(DISTRICT_NUMBER.getCode(), IndySpecialEducationFundingHeadcountHeader.DISTRICT_NAME.getCode(), IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NUMBER.getCode(), IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NAME.getCode(), MINCODE.getCode(), SCHOOL_NAME.getCode(),
                        REPORT_DATE.getCode(),
                        POSITIVE_CHANGE_LEVEL_1.getCode(),POSITIVE_CHANGE_LEVEL_2.getCode(), POSITIVE_CHANGE_LEVEL_3.getCode(), POSITIVE_CHANGE_SES.getCode(),
                        NET_CHANGE_LEVEL_1.getCode(), NET_CHANGE_LEVEL_2.getCode(), NET_CHANGE_LEVEL_3.getCode(), NET_CHANGE_SES.getCode(),
                        SEPT_LEVEL_1.getCode(), SEPT_LEVEL_2.getCode(), SEPT_LEVEL_3.getCode(), SEPT_SES.getCode(),
                        FEB_LEVEL_1.getCode(), FEB_LEVEL_2.getCode(), FEB_LEVEL_3.getCode(), FEB_SES.getCode())
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            var fundingReportTotals = new SpedFundingReportTotals();

            for(SpecialEdHeadcountResult februaryCollectionRecord: collectionRawData) {
                var septCollectionRecord = mappedSeptData.get(februaryCollectionRecord.getSchoolID());

                var schoolOpt = restUtils.getAllSchoolBySchoolID(februaryCollectionRecord.getSchoolID());
                if (schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    var districtOpt = restUtils.getDistrictByDistrictID(school.getDistrictId());

                    District district = null;
                    if (districtOpt.isPresent()) {
                        district = districtOpt.get();
                    }

                    Optional<IndependentAuthority> authorityOpt = Optional.empty();
                    if (school.getIndependentAuthorityId() != null) {
                        authorityOpt = restUtils.getAuthorityByAuthorityID(school.getIndependentAuthorityId());
                    }

                    IndependentAuthority authority = null;
                    if (authorityOpt.isPresent()) {
                        authority = authorityOpt.get();
                    }

                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        var positiveChangeLevel1 = TransformUtil.getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : "0", februaryCollectionRecord.getLevelOnes());
                        var positiveChangeLevel2 = TransformUtil.getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : "0", februaryCollectionRecord.getLevelTwos());
                        var positiveChangeLevel3 = TransformUtil.getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : "0", februaryCollectionRecord.getLevelThrees());
                        var positiveChangeSES = TransformUtil.getPositiveChange(septCollectionRecord != null ? septCollectionRecord.getOtherLevels() : "0", februaryCollectionRecord.getOtherLevels());
                        var netChangeLevel1 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : "0", februaryCollectionRecord.getLevelOnes());
                        var netChangeLevel2 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : "0", februaryCollectionRecord.getLevelTwos());
                        var netChangeLevel3 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : "0", februaryCollectionRecord.getLevelThrees());
                        var netChangeLevelSES = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getOtherLevels() : "0", februaryCollectionRecord.getOtherLevels());
                        List<String> csvRowData = prepareIndyInclusiveEdFundingDataForCsv(septCollectionRecord, februaryCollectionRecord, school,
                                authority, district, positiveChangeLevel1, positiveChangeLevel2, positiveChangeLevel3, positiveChangeSES,
                                netChangeLevel1, netChangeLevel2, netChangeLevel3, netChangeLevelSES, collection);
                        csvPrinter.printRecord(csvRowData);
                        if (septCollectionRecord != null) {
                            fundingReportTotals.setTotSeptLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel1s(), septCollectionRecord.getLevelOnes()));
                            fundingReportTotals.setTotSeptLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel2s(), septCollectionRecord.getLevelTwos()));
                            fundingReportTotals.setTotSeptLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel3s(), septCollectionRecord.getLevelThrees()));
                            fundingReportTotals.setTotSeptSES(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptSES(), septCollectionRecord.getOtherLevels()));
                        }
                        fundingReportTotals.setTotFebLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel1s(), februaryCollectionRecord.getLevelOnes()));
                        fundingReportTotals.setTotFebLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel2s(), februaryCollectionRecord.getLevelTwos()));
                        fundingReportTotals.setTotFebLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel3s(), februaryCollectionRecord.getLevelThrees()));
                        fundingReportTotals.setTotFebSES(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebSES(), februaryCollectionRecord.getOtherLevels()));
                        fundingReportTotals.setTotPositiveChangeLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel1s(), positiveChangeLevel1));
                        fundingReportTotals.setTotPositiveChangeLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel2s(), positiveChangeLevel2));
                        fundingReportTotals.setTotPositiveChangeLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel3s(), positiveChangeLevel3));
                        fundingReportTotals.setTotPositiveChangeSES(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeSES(), positiveChangeSES));
                        fundingReportTotals.setTotNetLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel1s(), netChangeLevel1));
                        fundingReportTotals.setTotNetLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel2s(), netChangeLevel2));
                        fundingReportTotals.setTotNetLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel3s(), netChangeLevel3));
                        fundingReportTotals.setTotNetSES(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetSES(), netChangeLevelSES));
                    }
                }
            }
            csvPrinter.printRecord(getIndependentSchoolFundingTotalRow(fundingReportTotals));
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(INDY_INCLUSIVE_ED_FUNDING_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private List<String> getIndependentSchoolFundingTotalRow(SpedFundingReportTotals totals){
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                "Totals",
                null,
                null,
                null,
                null,
                null,
                null,
                Double.toString(totals.getTotPositiveChangeLevel1s()),
                Double.toString(totals.getTotPositiveChangeLevel2s()),
                Double.toString(totals.getTotPositiveChangeLevel3s()),
                Double.toString(totals.getTotPositiveChangeSES()),
                Double.toString(totals.getTotNetLevel1s()),
                Double.toString(totals.getTotNetLevel2s()),
                Double.toString(totals.getTotNetLevel3s()),
                Double.toString(totals.getTotNetSES()),
                Double.toString(totals.getTotSeptLevel1s()),
                Double.toString(totals.getTotSeptLevel2s()),
                Double.toString(totals.getTotSeptLevel3s()),
                Double.toString(totals.getTotSeptSES()),
                Double.toString(totals.getTotFebLevel1s()),
                Double.toString(totals.getTotFebLevel2s()),
                Double.toString(totals.getTotFebLevel3s()),
                Double.toString(totals.getTotFebSES())
        ));
        return csvRowData;
    }

    // Independent School Headcount Enrolment by Funding Group and School and Grade report (I1005)
    public DownloadableReportResponse generateIndySchoolGradeFundingGroupEnrolledProgramHeadcounts(UUID collectionID) {
        List<IndySchoolGradeFundingGroupHeadcountResult> results = sdcSchoolCollectionStudentRepository.getIndySchoolGradeFundingGroupHeadcountsByCollectionId(collectionID);
        CollectionEntity collectionEntity = collectionRepository.findById(collectionID).orElseThrow(() -> new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString()));

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.DISTRICT_NUMBER.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.SCHOOL_NUMBER.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.SCHOOL_NAME.getCode(),
                        // Funding Groups
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_P.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_E.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_J.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_S.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_DL_K9.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.FUNDING_GROUP_DL_1012.getCode(),
                        // Core French
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_CF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_CF.getCode(),
                        // Programme Francophone
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_PF.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_PF.getCode(),
                        // Early French Immersion
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_EFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_EFI.getCode(),
                        // Late French Immersion
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_LFI.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_LFI.getCode(),
                        // English Language Learning
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_ELL.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_ELL.getCode(),
                        // Indigenous Language and Culture
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_ALC.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_ALC.getCode(),
                        // Indigenous Support Services
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_ASS.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_ASS.getCode(),
                        // Other Approved Indigenous Programs
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_HT_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.KIND_FT_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_01_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_02_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_03_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_04_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_05_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_06_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_07_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_EU_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_08_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_09_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_10_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_11_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_12_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_SU_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.GRADE_GA_OAAP.getCode(),
                        IndySchoolGradeEnrolmentFundingGroupHeadcountHeader.TOTAL_OAAP.getCode()
                )
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (IndySchoolGradeFundingGroupHeadcountResult result : results) {
                var schoolOpt = restUtils.getSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent()) {
                    var school = schoolOpt.get();
                    if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode())) {
                        List<String> csvRowData = prepareIndySchoolGradeFundingGroupDataForCsv(result, school, collectionEntity);
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(INDY_SCHOOL_GRADE_FUNDING_GROUP_ENROLLED_PROGRAMS_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    // Offshore School Enrolment Headcounts report
    public DownloadableReportResponse generateOffshoreSchoolsHeadcounts(UUID collectionID) {
        List<IndySchoolHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllIndyEnrollmentHeadcountsByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL.getCode(), KIND_FT.getCode(), GRADE_01.getCode(), GRADE_02.getCode(),
                        GRADE_03.getCode(),GRADE_04.getCode(),GRADE_05.getCode(),GRADE_06.getCode(),GRADE_07.getCode(),GRADE_EU.getCode(),
                        GRADE_08.getCode(),GRADE_09.getCode(), GRADE_10.getCode(), GRADE_11.getCode(),GRADE_12.getCode(),
                        GRADE_SU.getCode(),GRADE_GA.getCode(),GRADE_HS.getCode(), OffshoreSchoolEnrolmentHeadcountHeader.TOTAL.getCode())
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

    private Map<String, SpecialEdHeadcountResult> getLastSeptCollectionSchoolMap(CollectionEntity collection){
        var lastSeptCollectionOpt = sdcSchoolCollectionRepository.findLastCollectionByType(CollectionTypeCodes.SEPTEMBER.getTypeCode(), collection.getCollectionID(), collection.getSnapshotDate());
        if(lastSeptCollectionOpt.isEmpty()) {
            throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collection.getCollectionID().toString());
        }
        List<SpecialEdHeadcountResult> lastSeptCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsByCollectionId(lastSeptCollectionOpt.get().getCollectionID());
        return lastSeptCollectionRawData.stream().collect(Collectors.toMap(SpecialEdHeadcountResult::getSchoolID, item -> item));
    }

    private List<String> prepareIndyInclusiveEdFundingDataForCsv(SpecialEdHeadcountResult septCollectionRecord,
                                                                 SpecialEdHeadcountResult februaryCollectionRecord,
                                                                 School school,
                                                                 IndependentAuthority authority,
                                                                 District district,
                                                                 String positiveChangeLevel1,
                                                                 String positiveChangeLevel2,
                                                                 String positiveChangeLevel3,
                                                                 String positiveChangeSES,
                                                                 String netChangeLevel1,
                                                                 String netChangeLevel2,
                                                                 String netChangeLevel3,
                                                                 String netChangeSES,
                                                                 CollectionEntity collection) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getMincode(),
                school.getDisplayName(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),
                positiveChangeLevel1,
                positiveChangeLevel2,
                positiveChangeLevel3,
                positiveChangeSES,
                netChangeLevel1,
                netChangeLevel2,
                netChangeLevel3,
                netChangeSES,
                septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : "0",
                septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : "0",
                septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : "0",
                septCollectionRecord != null ? septCollectionRecord.getOtherLevels() : "0",
                februaryCollectionRecord.getLevelOnes(),
                februaryCollectionRecord.getLevelTwos(),
                februaryCollectionRecord.getLevelThrees(),
                februaryCollectionRecord.getOtherLevels()
        ));
        return csvRowData;
    }

    private List<String> prepareIndyInclusiveEdDataForCsv(IndySpecialEdAdultHeadcountResult result, School school, IndependentAuthority authority, CollectionEntity collection) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                authority != null ? authority.getAuthorityNumber() : "",
                authority != null ? authority.getDisplayName() : "",
                school.getMincode(),
                school.getDisplayName(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),
                result.getLevelOnes(),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdACodes(), result.getAdultsInSpecialEdA()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdBCodes(), result.getAdultsInSpecialEdB()),
                result.getLevelTwos(),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdCCodes(), result.getAdultsInSpecialEdC()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdDCodes(), result.getAdultsInSpecialEdD()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdECodes(), result.getAdultsInSpecialEdE()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdFCodes(), result.getAdultsInSpecialEdF()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdGCodes(), result.getAdultsInSpecialEdG()),
                result.getLevelThrees(),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdHCodes(), result.getAdultsInSpecialEdH()),
                result.getOtherLevels(),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdKCodes(), result.getAdultsInSpecialEdK()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdPCodes(), result.getAdultsInSpecialEdP()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdQCodes(), result.getAdultsInSpecialEdQ()),
                TransformUtil.flagSpecialEdHeadcountIfRequired(result.getSpecialEdRCodes(), result.getAdultsInSpecialEdR()),
                TransformUtil.getTotalHeadcount(result)
        ));
        return csvRowData;
    }

    // Offshore Spoken Language Headcounts report
    public DownloadableReportResponse generateOffshoreSpokenLanguageHeadcounts(UUID collectionID) {
        List<SpokenLanguageHeadcountResult> results = sdcSchoolCollectionStudentRepository.getAllHomeLanguageSpokenCodesForIndiesAndOffshoreInCollection(collectionID);

        List<SpokenLanguageHeadcountResult> offshoreSchoolResults = results.stream().filter(result -> {
            var school = restUtils.getSchoolBySchoolID(result.getSchoolID()).get();
            return school.getSchoolCategoryCode().equalsIgnoreCase(SchoolCategoryCodes.OFFSHORE.getCode());
        }).toList();

        var mappedHeaders = validationService.getActiveHomeLanguageSpokenCodes().stream().filter(languages ->
                        offshoreSchoolResults.stream().anyMatch(language -> language.getSpokenLanguageCode().equalsIgnoreCase(languages.getHomeLanguageSpokenCode())))
                .map(HomeLanguageSpokenCode::getDescription).sorted().toList();

        List<String> columns = new ArrayList<>();
        columns.add(SCHOOL.getCode());
        columns.addAll(mappedHeaders);

        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(columns.toArray(String[]::new))
                .build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (Map<String, String> map : prepareSpokenLanguageData(offshoreSchoolResults, mappedHeaders)) {
                List<String> csvRowData = new ArrayList<>(map.values());
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(OFFSHORE_SPOKEN_LANGUAGE_HEADCOUNTS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private ArrayList<LinkedHashMap<String, String>> prepareSpokenLanguageData(List<SpokenLanguageHeadcountResult> results, List<String> columns) {
        var rows = new ArrayList<LinkedHashMap<String, String>>();

        results.forEach(languageResult -> {
            var school = restUtils.getSchoolBySchoolID(languageResult.getSchoolID()).get();
                LinkedHashMap<String, String> rowMap = new LinkedHashMap<>();

                var existingRowOpt = rows.stream().filter(row -> row.containsValue(school.getSchoolId())).findFirst();
                if(existingRowOpt.isPresent()) {
                    //if school row already exist
                    var existingRow = existingRowOpt.get();
                    var spokenDesc = validationService.getActiveHomeLanguageSpokenCodes().stream()
                            .filter(code -> code.getHomeLanguageSpokenCode().equalsIgnoreCase(languageResult.getSpokenLanguageCode())).findFirst();
                    existingRow.put(spokenDesc.get().getDescription(), languageResult.getHeadcount());

                } else {
                    //create new rows
                    rowMap.put(SCHOOL.getCode(), school.getDisplayName());
                    rowMap.put(SCHOOL_ID, school.getSchoolId());
                    //look-up spoken language code and add its value
                    var spokenDesc = validationService.getActiveHomeLanguageSpokenCodes().stream()
                            .filter(code -> code.getHomeLanguageSpokenCode().equalsIgnoreCase(languageResult.getSpokenLanguageCode())).findFirst();
                    columns.forEach(column -> {
                        if(spokenDesc.get().getDescription().equalsIgnoreCase(column)) {
                            rowMap.put(spokenDesc.get().getDescription(), languageResult.getHeadcount());
                        } else {
                            rowMap.put(column, "0");
                        }
                    });
                    rows.add(rowMap);
                }
        });

        rows.forEach(row -> row.remove(SCHOOL_ID));
        return rows;
    }

    private List<String> prepareOffshoreSchoolDataForCsv(IndySchoolHeadcountResult result, School school) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                school.getDisplayName(),
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

    private List<String> prepareStudentDupeForCsv(SdcDuplicateStudentEntity dupeStud, SchoolTombstone school, District district, String duplicateType) {
        var student = dupeStud.getSdcSchoolCollectionStudentEntity();

        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                student.getAssignedPen(),
                district != null ? district.getDisplayName() : "-",
                school.getDisplayName(),
                school.getMincode(),
                student.getLocalID(),
                student.getDob(),
                student.getLegalLastName(),
                student.getLegalFirstName(),
                student.getLegalMiddleNames(),
                student.getUsualLastName(),
                student.getUsualFirstName(),
                student.getUsualMiddleNames(),
                student.getGender(),
                student.getPostalCode(),
                student.getIsAdult() != null ? student.getIsAdult().toString() : null,
                student.getIsGraduated() != null ? student.getIsGraduated().toString() : null,
                student.getEnrolledGradeCode(),
                student.getSchoolFundingCode(),
                student.getOtherCourses(),
                student.getSupportBlocks(),
                student.getYearsInEll() != null ? student.getYearsInEll().toString() : null,
                student.getCareerProgramCode(),
                student.getNativeAncestryInd(),
                student.getBandCode(),
                student.getSpecialEducationCategoryCode(),
                student.getFte() != null ? student.getFte().toString() : null,
                duplicateType
        ));
        return csvRowData;
    }

    private List<String> prepareAllSchoolDataForCsv(SchoolHeadcountResult schoolHeadcountResult, CollectionEntity collection, School school) {
        var schoolCategory = restUtils.getSchoolCategoryCode(school.getSchoolCategoryCode());
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());
        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                LocalDateTimeUtil.getSchoolYearString(collection),
                school.getMincode().substring(0,3),
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),
                schoolCategory.isPresent() ? schoolCategory.get().getLabel() : school.getSchoolCategoryCode(),
                TransformUtil.getGradesOfferedString(school),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),
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

    private List<String> prepareIndySchoolDataForCsv(IndySchoolHeadcountResult indySchoolHeadcountResult, School school, CollectionEntity collection) {
        List<String> schoolFundingGroupGrades;
        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            schoolFundingGroupGrades = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID()).stream().map(IndependentSchoolFundingGroupSnapshotEntity::getSchoolGradeCode).toList();
        }else{
            schoolFundingGroupGrades = school.getSchoolFundingGroups().stream().map(IndependentSchoolFundingGroup::getSchoolGradeCode).toList();
        }

        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                school.getDisplayName(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),
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

    private List<String> prepareIndySchoolGradeFundingGroupDataForCsv(IndySchoolGradeFundingGroupHeadcountResult indySchoolHeadcountResult, SchoolTombstone school, CollectionEntity collection) {
        Optional<District> districtOpt = restUtils.getDistrictByDistrictID(school.getDistrictId());
        String districtNumber = districtOpt.isPresent() ? districtOpt.get().getDistrictNumber() : "";

        List<IndependentSchoolFundingGroup> indyFundingGroups = restUtils.getSchoolFundingGroupsBySchoolID(school.getSchoolId());
        log.debug("School funding groups for school ID {}: {}", school.getSchoolId(), indyFundingGroups);

        return new ArrayList<>(Arrays.asList(
                districtNumber,
                school.getSchoolNumber(),
                school.getDisplayName(),
                // Funding Groups
                TransformUtil.getFundingGroupForGradesK3(indyFundingGroups),
                TransformUtil.getFundingGroupForGrades47(indyFundingGroups),
                TransformUtil.getFundingGroupForGrades810(indyFundingGroups),
                TransformUtil.getFundingGroupForGrades1112(indyFundingGroups),
                TransformUtil.getFundingGroupForDLGradesK9EU(indyFundingGroups, school.getFacilityTypeCode()),
                TransformUtil.getFundingGroupForDLGrades1012(indyFundingGroups, school.getFacilityTypeCode()),
                // Core French (08)
                indySchoolHeadcountResult.getKindHCountCF(),
                indySchoolHeadcountResult.getKindFCountCF(),
                indySchoolHeadcountResult.getGrade1CountCF(),
                indySchoolHeadcountResult.getGrade2CountCF(),
                indySchoolHeadcountResult.getGrade3CountCF(),
                indySchoolHeadcountResult.getGrade4CountCF(),
                indySchoolHeadcountResult.getGrade5CountCF(),
                indySchoolHeadcountResult.getGrade6CountCF(),
                indySchoolHeadcountResult.getGrade7CountCF(),
                indySchoolHeadcountResult.getGrade8CountCF(),
                indySchoolHeadcountResult.getGrade9CountCF(),
                indySchoolHeadcountResult.getGrade10CountCF(),
                indySchoolHeadcountResult.getGrade11CountCF(),
                indySchoolHeadcountResult.getGrade12CountCF(),
                indySchoolHeadcountResult.getGradeEUCountCF(),
                indySchoolHeadcountResult.getGradeSUCountCF(),
                indySchoolHeadcountResult.getGradeGACountCF(),
                TransformUtil.getTotalHeadcountCF(indySchoolHeadcountResult),
                // Programme Francophone (05)
                indySchoolHeadcountResult.getKindHCountPF(),
                indySchoolHeadcountResult.getKindFCountPF(),
                indySchoolHeadcountResult.getGrade1CountPF(),
                indySchoolHeadcountResult.getGrade2CountPF(),
                indySchoolHeadcountResult.getGrade3CountPF(),
                indySchoolHeadcountResult.getGrade4CountPF(),
                indySchoolHeadcountResult.getGrade5CountPF(),
                indySchoolHeadcountResult.getGrade6CountPF(),
                indySchoolHeadcountResult.getGrade7CountPF(),
                indySchoolHeadcountResult.getGrade8CountPF(),
                indySchoolHeadcountResult.getGrade9CountPF(),
                indySchoolHeadcountResult.getGrade10CountPF(),
                indySchoolHeadcountResult.getGrade11CountPF(),
                indySchoolHeadcountResult.getGrade12CountPF(),
                indySchoolHeadcountResult.getGradeEUCountPF(),
                indySchoolHeadcountResult.getGradeSUCountPF(),
                indySchoolHeadcountResult.getGradeGACountPF(),
                TransformUtil.getTotalHeadcountPF(indySchoolHeadcountResult),
                // Early French Immersion (11)
                indySchoolHeadcountResult.getKindHCountEFI(),
                indySchoolHeadcountResult.getKindFCountEFI(),
                indySchoolHeadcountResult.getGrade1CountEFI(),
                indySchoolHeadcountResult.getGrade2CountEFI(),
                indySchoolHeadcountResult.getGrade3CountEFI(),
                indySchoolHeadcountResult.getGrade4CountEFI(),
                indySchoolHeadcountResult.getGrade5CountEFI(),
                indySchoolHeadcountResult.getGrade6CountEFI(),
                indySchoolHeadcountResult.getGrade7CountEFI(),
                indySchoolHeadcountResult.getGrade8CountEFI(),
                indySchoolHeadcountResult.getGrade9CountEFI(),
                indySchoolHeadcountResult.getGrade10CountEFI(),
                indySchoolHeadcountResult.getGrade11CountEFI(),
                indySchoolHeadcountResult.getGrade12CountEFI(),
                indySchoolHeadcountResult.getGradeEUCountEFI(),
                indySchoolHeadcountResult.getGradeSUCountEFI(),
                indySchoolHeadcountResult.getGradeGACountEFI(),
                TransformUtil.getTotalHeadcountEFI(indySchoolHeadcountResult),
                // Late French Immersion (14)
                indySchoolHeadcountResult.getKindHCountLFI(),
                indySchoolHeadcountResult.getKindFCountLFI(),
                indySchoolHeadcountResult.getGrade1CountLFI(),
                indySchoolHeadcountResult.getGrade2CountLFI(),
                indySchoolHeadcountResult.getGrade3CountLFI(),
                indySchoolHeadcountResult.getGrade4CountLFI(),
                indySchoolHeadcountResult.getGrade5CountLFI(),
                indySchoolHeadcountResult.getGrade6CountLFI(),
                indySchoolHeadcountResult.getGrade7CountLFI(),
                indySchoolHeadcountResult.getGrade8CountLFI(),
                indySchoolHeadcountResult.getGrade9CountLFI(),
                indySchoolHeadcountResult.getGrade10CountLFI(),
                indySchoolHeadcountResult.getGrade11CountLFI(),
                indySchoolHeadcountResult.getGrade12CountLFI(),
                indySchoolHeadcountResult.getGradeEUCountLFI(),
                indySchoolHeadcountResult.getGradeSUCountLFI(),
                indySchoolHeadcountResult.getGradeGACountLFI(),
                TransformUtil.getTotalHeadcountLFI(indySchoolHeadcountResult),
                // English Language Learning (17)
                indySchoolHeadcountResult.getKindHCountELL(),
                indySchoolHeadcountResult.getKindFCountELL(),
                indySchoolHeadcountResult.getGrade1CountELL(),
                indySchoolHeadcountResult.getGrade2CountELL(),
                indySchoolHeadcountResult.getGrade3CountELL(),
                indySchoolHeadcountResult.getGrade4CountELL(),
                indySchoolHeadcountResult.getGrade5CountELL(),
                indySchoolHeadcountResult.getGrade6CountELL(),
                indySchoolHeadcountResult.getGrade7CountELL(),
                indySchoolHeadcountResult.getGrade8CountELL(),
                indySchoolHeadcountResult.getGrade9CountELL(),
                indySchoolHeadcountResult.getGrade10CountELL(),
                indySchoolHeadcountResult.getGrade11CountELL(),
                indySchoolHeadcountResult.getGrade12CountELL(),
                indySchoolHeadcountResult.getGradeEUCountELL(),
                indySchoolHeadcountResult.getGradeSUCountELL(),
                indySchoolHeadcountResult.getGradeGACountELL(),
                TransformUtil.getTotalHeadcountELL(indySchoolHeadcountResult),
                // Indigenous Language and Culture (29)
                indySchoolHeadcountResult.getKindHCountALC(),
                indySchoolHeadcountResult.getKindFCountALC(),
                indySchoolHeadcountResult.getGrade1CountALC(),
                indySchoolHeadcountResult.getGrade2CountALC(),
                indySchoolHeadcountResult.getGrade3CountALC(),
                indySchoolHeadcountResult.getGrade4CountALC(),
                indySchoolHeadcountResult.getGrade5CountALC(),
                indySchoolHeadcountResult.getGrade6CountALC(),
                indySchoolHeadcountResult.getGrade7CountALC(),
                indySchoolHeadcountResult.getGrade8CountALC(),
                indySchoolHeadcountResult.getGrade9CountALC(),
                indySchoolHeadcountResult.getGrade10CountALC(),
                indySchoolHeadcountResult.getGrade11CountALC(),
                indySchoolHeadcountResult.getGrade12CountALC(),
                indySchoolHeadcountResult.getGradeEUCountALC(),
                indySchoolHeadcountResult.getGradeSUCountALC(),
                indySchoolHeadcountResult.getGradeGACountALC(),
                TransformUtil.getTotalHeadcountALC(indySchoolHeadcountResult),
                // Indigenous Support Services (33)
                indySchoolHeadcountResult.getKindHCountASS(),
                indySchoolHeadcountResult.getKindFCountASS(),
                indySchoolHeadcountResult.getGrade1CountASS(),
                indySchoolHeadcountResult.getGrade2CountASS(),
                indySchoolHeadcountResult.getGrade3CountASS(),
                indySchoolHeadcountResult.getGrade4CountASS(),
                indySchoolHeadcountResult.getGrade5CountASS(),
                indySchoolHeadcountResult.getGrade6CountASS(),
                indySchoolHeadcountResult.getGrade7CountASS(),
                indySchoolHeadcountResult.getGrade8CountASS(),
                indySchoolHeadcountResult.getGrade9CountASS(),
                indySchoolHeadcountResult.getGrade10CountASS(),
                indySchoolHeadcountResult.getGrade11CountASS(),
                indySchoolHeadcountResult.getGrade12CountASS(),
                indySchoolHeadcountResult.getGradeEUCountASS(),
                indySchoolHeadcountResult.getGradeSUCountASS(),
                indySchoolHeadcountResult.getGradeGACountASS(),
                TransformUtil.getTotalHeadcountASS(indySchoolHeadcountResult),
                // Other Approved Indigenous Programs (36)
                indySchoolHeadcountResult.getKindHCountOAAP(),
                indySchoolHeadcountResult.getKindFCountOAAP(),
                indySchoolHeadcountResult.getGrade1CountOAAP(),
                indySchoolHeadcountResult.getGrade2CountOAAP(),
                indySchoolHeadcountResult.getGrade3CountOAAP(),
                indySchoolHeadcountResult.getGrade4CountOAAP(),
                indySchoolHeadcountResult.getGrade5CountOAAP(),
                indySchoolHeadcountResult.getGrade6CountOAAP(),
                indySchoolHeadcountResult.getGrade7CountOAAP(),
                indySchoolHeadcountResult.getGrade8CountOAAP(),
                indySchoolHeadcountResult.getGrade9CountOAAP(),
                indySchoolHeadcountResult.getGrade10CountOAAP(),
                indySchoolHeadcountResult.getGrade11CountOAAP(),
                indySchoolHeadcountResult.getGrade12CountOAAP(),
                indySchoolHeadcountResult.getGradeEUCountOAAP(),
                indySchoolHeadcountResult.getGradeSUCountOAAP(),
                indySchoolHeadcountResult.getGradeGACountOAAP(),
                TransformUtil.getTotalHeadcountOAAP(indySchoolHeadcountResult)
        ));
    }

    private List<String> prepareFsaDataForCsv(SdcSchoolCollectionStudentFsaReportEntity student, String studentGrade, SchoolTombstone school) {

        List<String> csvRowData = new ArrayList<>();
        csvRowData.addAll(Arrays.asList(
                school.getMincode(),
                student.getAssignedPen(),
                studentGrade,
                student.getLegalFirstName(),
                student.getLegalLastName()
            ));
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

    // Inclusive Ed Variance Report for all
    public DownloadableReportResponse generateInclusiveEducationVarianceReport(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        List<String> headers = Arrays.stream(InclusiveEducationVarianceHeader.values()).map(InclusiveEducationVarianceHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .build();

        var districtCollections = sdcDistrictCollectionRepository.findAllByCollectionEntityCollectionID(collectionID);
        List<UUID> includeSchoolForEnrolledHeadcountsAndFteReport = restUtils.getAllSchoolTombstones().stream()
                .filter(this::shouldIncludeSchoolForEnrolledHeadcountsAndFteReport)
                .map(SchoolTombstone::getSchoolId)
                .filter(Objects::nonNull)
                .map(UUID::fromString)
                .toList();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for(SdcDistrictCollectionEntity sdcDistrictCollectionEntity: districtCollections){
                UUID districtID = sdcDistrictCollectionEntity.getDistrictID();
                // Get previous February collection relative to given collectionID
                SdcDistrictCollectionEntity febCollection = sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(CollectionTypeCodes.FEBRUARY.getTypeCode(), districtID, sdcDistrictCollectionEntity.getCollectionEntity().getSnapshotDate())
                        .orElseThrow(() -> new RuntimeException("No previous or current February sdc district collection found."));

                // Get previous September collection relative to previous February collection
                SdcDistrictCollectionEntity septCollection = sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(CollectionTypeCodes.SEPTEMBER.getTypeCode(), districtID, febCollection.getCollectionEntity().getSnapshotDate())
                        .orElseThrow(() -> new RuntimeException("No previous September sdc district collection found relative to the February collection."));

                var febCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleBySdcDistrictCollectionId(febCollection.getSdcDistrictCollectionID(), includeSchoolForEnrolledHeadcountsAndFteReport);
                var septCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsVarianceSimpleBySdcDistrictCollectionId(septCollection.getSdcDistrictCollectionID(), includeSchoolForEnrolledHeadcountsAndFteReport);

                var district = restUtils.getDistrictByDistrictID(districtID.toString()).orElseThrow(() -> new EntityNotFoundException(District.class, DISTRICT_ID, districtID.toString()));

                List<String> csvRowData = prepareInclusiveEducationVarianceForCsv(septCollectionRawData, febCollectionRawData, district);
                csvPrinter.printRecord(csvRowData);
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(INCLUSIVE_EDUCATION_VARIANCES_ALL.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    // ISFS Preliminary report
    public DownloadableReportResponse generateISFSReport(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        List<ISFSPrelimHeadcountResult> results;
        if(Objects.equals(collectionOpt.get().getCollectionTypeCode(), CollectionTypeCodes.FEBRUARY.getTypeCode())){
            results = sdcSchoolCollectionStudentRepository.getISFSPreliminaryDataFebruaryByCollectionId(collectionID);
        } else {
            results = sdcSchoolCollectionStudentRepository.getISFSPreliminaryDataByCollectionId(collectionID);
        }


        List<String> headers = Arrays.stream(ISFSPreliminaryHeader.values()).map(ISFSPreliminaryHeader::getCode).toList();
        var csvFormat = CSVFormat.Builder.create()
                .setHeader(headers.toArray(String[]::new))
                .setQuoteMode(QuoteMode.ALL)
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            Collections.sort(results, new Comparator<ISFSPrelimHeadcountResult>() {
                @Override
                public int compare(ISFSPrelimHeadcountResult res1, ISFSPrelimHeadcountResult res2) {
                    var school1 = restUtils.getSchoolBySchoolID(res1.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, res1.getSchoolID()));
                    var school2 = restUtils.getSchoolBySchoolID(res2.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, res2.getSchoolID()));
                    var district1 = restUtils.getDistrictByDistrictID(school1.getDistrictId()).orElseThrow(() -> new EntityNotFoundException(District.class, DISTRICT_ID, school1.getDistrictId()));
                    var district2 = restUtils.getDistrictByDistrictID(school2.getDistrictId()).orElseThrow(() -> new EntityNotFoundException(District.class, DISTRICT_ID, school2.getDistrictId()));
                    if(Integer.parseInt(district1.getDistrictNumber()) > Integer.parseInt(district2.getDistrictNumber())){
                        return 1;
                    }else if(Integer.parseInt(district1.getDistrictNumber()) < Integer.parseInt(district2.getDistrictNumber())){
                        return -1;
                    }else if(Integer.parseInt(school1.getSchoolNumber()) > Integer.parseInt(school2.getSchoolNumber())){
                        return 1;
                    }else if(Integer.parseInt(school1.getSchoolNumber()) < Integer.parseInt(school2.getSchoolNumber())){
                        return -1;
                    }
                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                    return  0;
                }
            });

            for (ISFSPrelimHeadcountResult result : results) {
                var school = restUtils.getAllSchoolBySchoolID(result.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, result.getSchoolID()));
                if (SchoolCategoryCodes.INDEPENDENTS.contains(school.getSchoolCategoryCode()) && schoolHasValidFundingCodes(school, collectionOpt.get())) {
                    var prelimRes = getPrelimFinalResult(result, school);
                    var hasStudents = schoolHasAnyStudents(prelimRes);
                    if(hasStudents) {
                        List<String> csvRowData = prepareISFSPrelimDataForCsv(prelimRes, school, collectionOpt.get());
                        csvPrinter.printRecord(csvRowData);
                    }
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(ISFS_PRELIMINARY_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private boolean schoolHasAnyStudents(ISFSPrelimHeadcountFinalResult headcountResult){
        var specialEducationLevel1Count = StringUtils.isBlank(headcountResult.getSpecialEducationLevel1Count()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getSpecialEducationLevel1Count());
        var specialEducationLevel2Count = StringUtils.isBlank(headcountResult.getSpecialEducationLevel2Count()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getSpecialEducationLevel2Count());
        var specialEducationLevel3Count = StringUtils.isBlank(headcountResult.getSpecialEducationLevel3Count()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getSpecialEducationLevel3Count());
        var specialEducationLevelOtherCount = StringUtils.isBlank(headcountResult.getSpecialEducationLevelOtherCount()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getSpecialEducationLevelOtherCount());
        var standardAdultsKto3Fte = StringUtils.isBlank(headcountResult.getStandardAdultsKto3Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardAdultsKto3Fte());
        var standardAdults4to7EUFte = StringUtils.isBlank(headcountResult.getStandardAdults4to7EUFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardAdults4to7EUFte());
        var standardAdults8to10SUFte = StringUtils.isBlank(headcountResult.getStandardAdults8to10SUFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardAdults8to10SUFte());
        var standardAdults11and12Fte = StringUtils.isBlank(headcountResult.getStandardAdults11and12Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardAdults11and12Fte());
        var dLAdultsKto9Fte = StringUtils.isBlank(headcountResult.getDLAdultsKto9Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getDLAdultsKto9Fte());
        var dLAdults10to12Fte = StringUtils.isBlank(headcountResult.getDLAdults10to12Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getDLAdults10to12Fte());
        var standardSchoolAgedKHFte = StringUtils.isBlank(headcountResult.getStandardSchoolAgedKHFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAgedKHFte());
        var standardSchoolAgedKFFte = StringUtils.isBlank(headcountResult.getStandardSchoolAgedKFFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAgedKFFte());
        var standardSchoolAged1to3Fte = StringUtils.isBlank(headcountResult.getStandardSchoolAged1to3Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAged1to3Fte());
        var standardSchoolAged4to7EUFte = StringUtils.isBlank(headcountResult.getStandardSchoolAged4to7EUFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAged4to7EUFte());
        var standardSchoolAged8to10SUFte = StringUtils.isBlank(headcountResult.getStandardSchoolAged8to10SUFte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAged8to10SUFte());
        var standardSchoolAged11and12Fte = StringUtils.isBlank(headcountResult.getStandardSchoolAged11and12Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getStandardSchoolAged11and12Fte());
        var dLSchoolAgedKto9Fte = StringUtils.isBlank(headcountResult.getDLSchoolAgedKto9Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getDLSchoolAgedKto9Fte());
        var dLSchoolAged10to12Fte = StringUtils.isBlank(headcountResult.getDLSchoolAged10to12Fte()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getDLSchoolAged10to12Fte());
        var totalHomeschoolCount = StringUtils.isBlank(headcountResult.getTotalHomeschoolCount()) ? new BigDecimal(0) : new BigDecimal(headcountResult.getTotalHomeschoolCount());

        return specialEducationLevel1Count.compareTo(new BigDecimal(0)) != 0 ||
                specialEducationLevel2Count.compareTo(new BigDecimal(0)) != 0 ||
                specialEducationLevel3Count.compareTo(new BigDecimal(0)) != 0 ||
                specialEducationLevelOtherCount.compareTo(new BigDecimal(0)) != 0 ||
                standardAdultsKto3Fte.compareTo(new BigDecimal(0)) != 0 ||
                standardAdults4to7EUFte.compareTo(new BigDecimal(0)) != 0 ||
                standardAdults8to10SUFte.compareTo(new BigDecimal(0)) != 0 ||
                standardAdults11and12Fte.compareTo(new BigDecimal(0)) != 0 ||
                dLAdultsKto9Fte.compareTo(new BigDecimal(0)) != 0 ||
                dLAdults10to12Fte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAgedKHFte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAgedKFFte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAged1to3Fte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAged4to7EUFte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAged8to10SUFte.compareTo(new BigDecimal(0)) != 0 ||
                standardSchoolAged11and12Fte.compareTo(new BigDecimal(0)) != 0 ||
                dLSchoolAgedKto9Fte.compareTo(new BigDecimal(0)) != 0 ||
                dLSchoolAged10to12Fte.compareTo(new BigDecimal(0)) != 0 ||
                totalHomeschoolCount.compareTo(new BigDecimal(0)) != 0;
    }

    private boolean schoolHasValidFundingCodes(School school, CollectionEntity collection){
        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID());
            for (IndependentSchoolFundingGroupSnapshotEntity fundingGroup : schoolFundingGroups) {
                if (fundingGroup.getSchoolFundingGroupCode().equalsIgnoreCase(GROUP_1) || fundingGroup.getSchoolFundingGroupCode().equalsIgnoreCase(GROUP_2)) {
                    return true;
                }
            }
        }else{
            for (IndependentSchoolFundingGroup fundingGroup : school.getSchoolFundingGroups()) {
                if (fundingGroup.getSchoolFundingGroupCode().equalsIgnoreCase(GROUP_1) || fundingGroup.getSchoolFundingGroupCode().equalsIgnoreCase(GROUP_2)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ISFSPrelimHeadcountFinalResult getPrelimFinalResult(ISFSPrelimHeadcountResult queryResult, School school){
        ISFSPrelimHeadcountFinalResult finalResult = new ISFSPrelimHeadcountFinalResult();

        BigDecimal finalSpedLevel1KHCount = queryResult.getSpecialEducationLevel1CountKH() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel1CountKH());
        BigDecimal finalSpedLevel2KHCount = queryResult.getSpecialEducationLevel2CountKH() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel2CountKH());
        BigDecimal finalSpedLevel3KHCount = queryResult.getSpecialEducationLevel3CountKH() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel3CountKH());
        BigDecimal finalSpedLevelSESKHCount = queryResult.getSpecialEducationLevelOtherCountKH() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevelOtherCountKH());

        BigDecimal finalSpedLevel1Count = queryResult.getSpecialEducationLevel1Count() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel1Count());
        BigDecimal finalSpedLevel2Count = queryResult.getSpecialEducationLevel2Count() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel2Count());
        BigDecimal finalSpedLevel3Count = queryResult.getSpecialEducationLevel3Count() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevel3Count());
        BigDecimal finalSpedLevelSESCount = queryResult.getSpecialEducationLevelOtherCount() == null ? new BigDecimal(0) : new BigDecimal(queryResult.getSpecialEducationLevelOtherCount());

        finalResult.setSpecialEducationLevel1Count(finalSpedLevel1KHCount.add(finalSpedLevel1Count).toString());
        finalResult.setSpecialEducationLevel2Count(finalSpedLevel2KHCount.add(finalSpedLevel2Count).toString());
        finalResult.setSpecialEducationLevel3Count(finalSpedLevel3KHCount.add(finalSpedLevel3Count).toString());
        finalResult.setSpecialEducationLevelOtherCount(finalSpedLevelSESKHCount.add(finalSpedLevelSESCount).toString());

        if(school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode())) {
            finalResult.setDLAdultsKto9Fte(queryResult.getAdultsKto9Fte());
            finalResult.setDLAdults10to12Fte(queryResult.getAdults10to12Fte());
            finalResult.setDLSchoolAgedKto9Fte(queryResult.getSchoolAgedKto9Fte());
            finalResult.setDLSchoolAged10to12Fte(queryResult.getSchoolAged10to12Fte());
        }else{
            finalResult.setStandardAdultsKto3Fte(queryResult.getAdultsKto3Fte());
            finalResult.setStandardAdults4to7EUFte(queryResult.getAdults4to7EUFte());
            finalResult.setStandardAdults8to10SUFte(queryResult.getAdults8to10SUFte());
            finalResult.setStandardAdults11and12Fte(queryResult.getAdults11and12Fte());

            finalResult.setStandardSchoolAgedKHFte(queryResult.getSchoolAgedKHFte());
            finalResult.setStandardSchoolAgedKFFte(queryResult.getSchoolAgedKFFte());
            finalResult.setStandardSchoolAged1to3Fte(queryResult.getSchoolAged1to3Fte());
            finalResult.setStandardSchoolAged4to7EUFte(queryResult.getSchoolAged4to7EUFte());
            finalResult.setStandardSchoolAged8to10SUFte(queryResult.getSchoolAged8to10SUFte());
            finalResult.setStandardSchoolAged11and12Fte(queryResult.getSchoolAged11and12Fte());
        }

        finalResult.setTotalHomeschoolCount(queryResult.getTotalHomeschoolCount());

        return finalResult;
    }

    // Enroled Headcounts and FTEs by School report
    public DownloadableReportResponse generateEnrolledHeadcountsAndFteReport(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        List<EnrolmentHeadcountFteResult> results;

        if(Objects.equals(collectionOpt.get().getCollectionTypeCode(), CollectionTypeCodes.FEBRUARY.getTypeCode())){
            results = sdcSchoolCollectionStudentRepository.getEnrolmentHeadcountsAndFteByFebCollectionId(collectionID);
        } else {
            results = sdcSchoolCollectionStudentRepository.getEnrolmentHeadcountsAndFteByCollectionId(collectionID);
        }

        List<String> headers = Arrays.stream(EnrolmentAndFteHeader.values()).map(EnrolmentAndFteHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (EnrolmentHeadcountFteResult result : results) {
                var school = restUtils.getSchoolBySchoolID(result.getSchoolID()).orElseThrow(() -> new EntityNotFoundException(SchoolTombstone.class, SCHOOL_ID, result.getSchoolID()));

                if(shouldIncludeSchoolForEnrolledHeadcountsAndFteReport(school)) {
                    List<String> csvRowData = prepareEnrolmentFteDataForCsv(result, school);
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(ENROLLED_HEADCOUNTS_AND_FTE_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private boolean shouldIncludeSchoolForEnrolledHeadcountsAndFteReport(SchoolTombstone school){
        var invalidSchoolCategories = new String[]{SchoolCategoryCodes.INDEPEND.getCode(), SchoolCategoryCodes.INDP_FNS.getCode(), SchoolCategoryCodes.OFFSHORE.getCode()};
        var invalidFacilityTypes = new String[]{FacilityTypeCodes.LONG_PRP.getCode(), FacilityTypeCodes.SHORT_PRP.getCode(), FacilityTypeCodes.YOUTH.getCode()};
        var categoryCode = school.getSchoolCategoryCode();
        var facilityType = school.getFacilityTypeCode();
        return Arrays.stream(invalidSchoolCategories).noneMatch(categoryCode::equals) && Arrays.stream(invalidFacilityTypes).noneMatch(facilityType::equals);
    }

    // Refugee Enroled Headcounts and FTEs report
    public DownloadableReportResponse generateRefugeeEnrolmentHeadcountsAndFteReport(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CollectionEntity collection = collectionOpt.get();
        if(!collection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.FEBRUARY.getTypeCode())) {
            throw new InvalidPayloadException(createError(REFUGEE_HEADCOUNTS_INVALID_COLLECTION_TYPE));
        }

        List<EnrolmentHeadcountFteResult> results = sdcSchoolCollectionStudentRepository.getNewRefugeeEnrolmentHeadcountsAndFteWithByCollectionId(collectionID);

        List<String> headers = Arrays.stream(RefugeeEnrolmentAndFteHeader.values()).map(RefugeeEnrolmentAndFteHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (EnrolmentHeadcountFteResult result : results) {
                var schoolOpt = restUtils.getSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent() &&
                        (schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.STANDARD.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.ALT_PROGS.getCode()))) {
                    List<String> csvRowData = prepareRefugeeEnrolmentFteData(result, schoolOpt.get());
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(REFUGEE_ENROLMENT_HEADCOUNTS_AND_FTE_REPORT.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));
            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
    }

    private ApiError createError(String message) {
        return ApiError.builder().timestamp(LocalDateTime.now()).message(message).status(BAD_REQUEST).build();
    }

    private BigDecimal calculateVariance(String septCount, String febCount) {
        if(septCount == null){
            septCount = "0";
        }
        if(febCount == null){
            febCount = "0";
        }
        BigDecimal feb = new BigDecimal(febCount);
        BigDecimal sept = new BigDecimal(septCount);
        return feb.subtract(sept);
    }

    private List<String> prepareInclusiveEducationVarianceForCsv(SpecialEdHeadcountResult septResult, SpecialEdHeadcountResult febResult, District district) {
        return new ArrayList<>(Arrays.asList(
                district.getDistrictNumber(),
                district.getDisplayName(),
                septResult.getSpecialEdACodes(),
                septResult.getSpecialEdBCodes(),
                septResult.getSpecialEdCCodes(),
                septResult.getSpecialEdDCodes(),
                septResult.getSpecialEdECodes(),
                septResult.getSpecialEdFCodes(),
                septResult.getSpecialEdGCodes(),
                septResult.getSpecialEdHCodes(),
                septResult.getSpecialEdKCodes(),
                septResult.getSpecialEdPCodes(),
                septResult.getSpecialEdQCodes(),
                septResult.getSpecialEdRCodes(),
                febResult.getSpecialEdACodes(),
                febResult.getSpecialEdBCodes(),
                febResult.getSpecialEdCCodes(),
                febResult.getSpecialEdDCodes(),
                febResult.getSpecialEdECodes(),
                febResult.getSpecialEdFCodes(),
                febResult.getSpecialEdGCodes(),
                febResult.getSpecialEdHCodes(),
                febResult.getSpecialEdKCodes(),
                febResult.getSpecialEdPCodes(),
                febResult.getSpecialEdQCodes(),
                febResult.getSpecialEdRCodes(),
                calculateVariance(septResult.getSpecialEdACodes(), febResult.getSpecialEdACodes()).toString(),
                calculateVariance(septResult.getSpecialEdBCodes(), febResult.getSpecialEdBCodes()).toString(),
                calculateVariance(septResult.getSpecialEdCCodes(), febResult.getSpecialEdCCodes()).toString(),
                calculateVariance(septResult.getSpecialEdDCodes(), febResult.getSpecialEdDCodes()).toString(),
                calculateVariance(septResult.getSpecialEdECodes(), febResult.getSpecialEdECodes()).toString(),
                calculateVariance(septResult.getSpecialEdFCodes(), febResult.getSpecialEdFCodes()).toString(),
                calculateVariance(septResult.getSpecialEdGCodes(), febResult.getSpecialEdGCodes()).toString(),
                calculateVariance(septResult.getSpecialEdHCodes(), febResult.getSpecialEdHCodes()).toString(),
                calculateVariance(septResult.getSpecialEdKCodes(), febResult.getSpecialEdKCodes()).toString(),
                calculateVariance(septResult.getSpecialEdPCodes(), febResult.getSpecialEdPCodes()).toString(),
                calculateVariance(septResult.getSpecialEdQCodes(), febResult.getSpecialEdQCodes()).toString(),
                calculateVariance(septResult.getSpecialEdRCodes(), febResult.getSpecialEdRCodes()).toString()
        ));
    }

    private List<String> prepareISFSPrelimDataForCsv(ISFSPrelimHeadcountFinalResult headcountResult, School school, CollectionEntity collection) {
        String groupStandardPrimary = null;
        String groupStandardElementary = null;
        String groupStandardJunior = null;
        String groupStandardSecondary = null;
        String groupDLPrimary = null;
        String groupDLSecondary = null;
        String postedYN = null;

        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            postedYN = "Y";
            List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID());

            if(school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode())) {
                groupDLPrimary = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups,
                        Arrays.asList(SchoolGradeCodes.KINDFULL.getTypeCode(),
                                SchoolGradeCodes.KINDHALF.getTypeCode(),
                                SchoolGradeCodes.GRADE01.getTypeCode(),
                                SchoolGradeCodes.GRADE02.getTypeCode(),
                                SchoolGradeCodes.GRADE03.getTypeCode(),
                                SchoolGradeCodes.GRADE04.getTypeCode(),
                                SchoolGradeCodes.GRADE05.getTypeCode(),
                                SchoolGradeCodes.GRADE06.getTypeCode(),
                                SchoolGradeCodes.GRADE07.getTypeCode(),
                                SchoolGradeCodes.GRADE08.getTypeCode(),
                                SchoolGradeCodes.GRADE09.getTypeCode(),
                                SchoolGradeCodes.ELEMUNGR.getTypeCode()));
                groupDLSecondary = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups,
                        Arrays.asList(SchoolGradeCodes.GRADE10.getTypeCode(),
                                SchoolGradeCodes.GRADE11.getTypeCode(),
                                SchoolGradeCodes.GRADE12.getTypeCode(),
                                SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode()));
            }else{
                groupStandardPrimary = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.KINDFULL.getTypeCode(), SchoolGradeCodes.KINDHALF.getTypeCode(), SchoolGradeCodes.GRADE01.getTypeCode(), SchoolGradeCodes.GRADE02.getTypeCode(), SchoolGradeCodes.GRADE03.getTypeCode()));
                groupStandardElementary = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE04.getTypeCode(), SchoolGradeCodes.GRADE05.getTypeCode(), SchoolGradeCodes.GRADE06.getTypeCode(), SchoolGradeCodes.GRADE07.getTypeCode(), SchoolGradeCodes.ELEMUNGR.getTypeCode()));
                groupStandardJunior = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE08.getTypeCode(), SchoolGradeCodes.GRADE09.getTypeCode(), SchoolGradeCodes.GRADE10.getTypeCode(), SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode()));
                groupStandardSecondary = TransformUtil.getLowestFundingGroupSnapshotForGroup(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE11.getTypeCode(), SchoolGradeCodes.GRADE12.getTypeCode()));
            }
        }else{
            postedYN = "N";
            List<IndependentSchoolFundingGroup> schoolFundingGroups = school.getSchoolFundingGroups();
            if(school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()) || school.getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode())) {
                groupDLPrimary = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups,
                        Arrays.asList(SchoolGradeCodes.KINDFULL.getTypeCode(),
                                SchoolGradeCodes.KINDHALF.getTypeCode(),
                                SchoolGradeCodes.GRADE01.getTypeCode(),
                                SchoolGradeCodes.GRADE02.getTypeCode(),
                                SchoolGradeCodes.GRADE03.getTypeCode(),
                                SchoolGradeCodes.GRADE04.getTypeCode(),
                                SchoolGradeCodes.GRADE05.getTypeCode(),
                                SchoolGradeCodes.GRADE06.getTypeCode(),
                                SchoolGradeCodes.GRADE07.getTypeCode(),
                                SchoolGradeCodes.GRADE08.getTypeCode(),
                                SchoolGradeCodes.GRADE09.getTypeCode(),
                                SchoolGradeCodes.ELEMUNGR.getTypeCode()));
                groupDLSecondary = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups,
                        Arrays.asList(SchoolGradeCodes.GRADE10.getTypeCode(),
                                SchoolGradeCodes.GRADE11.getTypeCode(),
                                SchoolGradeCodes.GRADE12.getTypeCode(),
                                SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode()));
            }else{
                groupStandardPrimary = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.KINDFULL.getTypeCode(), SchoolGradeCodes.KINDHALF.getTypeCode(), SchoolGradeCodes.GRADE01.getTypeCode(), SchoolGradeCodes.GRADE02.getTypeCode(), SchoolGradeCodes.GRADE03.getTypeCode()));
                groupStandardElementary = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE04.getTypeCode(), SchoolGradeCodes.GRADE05.getTypeCode(), SchoolGradeCodes.GRADE06.getTypeCode(), SchoolGradeCodes.GRADE07.getTypeCode(), SchoolGradeCodes.ELEMUNGR.getTypeCode()));
                groupStandardJunior = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE08.getTypeCode(), SchoolGradeCodes.GRADE09.getTypeCode(), SchoolGradeCodes.GRADE10.getTypeCode(), SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode()));
                groupStandardSecondary = TransformUtil.getLowestFundingGroupForGrade(schoolFundingGroups, Arrays.asList(SchoolGradeCodes.GRADE11.getTypeCode(), SchoolGradeCodes.GRADE12.getTypeCode()));
            }
        }

        return new ArrayList<>(Arrays.asList(
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),
                school.getMincode().substring(0, 3),
                school.getSchoolNumber(),
                groupStandardPrimary == null ? " " : groupStandardPrimary,
                groupStandardElementary == null ? " " : groupStandardElementary,
                groupStandardJunior == null ? " " : groupStandardJunior,
                groupStandardSecondary == null ? " " : groupStandardSecondary,
                groupDLPrimary == null ? " " : groupDLPrimary,
                groupDLSecondary == null ? " " : groupDLSecondary,
                headcountResult.getSpecialEducationLevel1Count() == null ? "0" : stripTrailingZeros(headcountResult.getSpecialEducationLevel1Count()),
                headcountResult.getSpecialEducationLevel2Count() == null ? "0" : stripTrailingZeros(headcountResult.getSpecialEducationLevel2Count()),
                headcountResult.getSpecialEducationLevel3Count() == null ? "0" : stripTrailingZeros(headcountResult.getSpecialEducationLevel3Count()),
                headcountResult.getSpecialEducationLevelOtherCount() == null ? "0" : stripTrailingZeros(headcountResult.getSpecialEducationLevelOtherCount()),
                headcountResult.getStandardAdultsKto3Fte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardAdultsKto3Fte()),
                headcountResult.getStandardAdults4to7EUFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardAdults4to7EUFte()),
                headcountResult.getStandardAdults8to10SUFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardAdults8to10SUFte()),
                headcountResult.getStandardAdults11and12Fte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardAdults11and12Fte()),
                headcountResult.getDLAdultsKto9Fte() == null ? "0" : stripTrailingZeros(headcountResult.getDLAdultsKto9Fte()),
                headcountResult.getDLAdults10to12Fte() == null ? "0" : stripTrailingZeros(headcountResult.getDLAdults10to12Fte()),
                headcountResult.getStandardSchoolAgedKHFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAgedKHFte()),
                headcountResult.getStandardSchoolAgedKFFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAgedKFFte()),
                headcountResult.getStandardSchoolAged1to3Fte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAged1to3Fte()),
                headcountResult.getStandardSchoolAged4to7EUFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAged4to7EUFte()),
                headcountResult.getStandardSchoolAged8to10SUFte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAged8to10SUFte()),
                headcountResult.getStandardSchoolAged11and12Fte() == null ? "0" : stripTrailingZeros(headcountResult.getStandardSchoolAged11and12Fte()),
                headcountResult.getDLSchoolAgedKto9Fte() == null ? "0" : stripTrailingZeros(headcountResult.getDLSchoolAgedKto9Fte()),
                headcountResult.getDLSchoolAged10to12Fte() == null ? "0" : stripTrailingZeros(headcountResult.getDLSchoolAged10to12Fte()),
                headcountResult.getTotalHomeschoolCount() == null ? "0" : stripTrailingZeros(headcountResult.getTotalHomeschoolCount()),
                postedYN
        ));
    }

    private static String stripTrailingZeros(String s){
        if(StringUtils.isNotBlank(s)) {
            return s.contains(".") ? s.replaceAll("0*$", "").replaceAll("\\.$", "") : s;
        }
        return null;
    }

    private List<String> prepareEnrolmentFteDataForCsv(EnrolmentHeadcountFteResult headcountResult, SchoolTombstone school) {
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());
        return new ArrayList<>(Arrays.asList(
                school.getMincode().substring(0, 3),
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),
                headcountResult.getKhTotalCount(),
                headcountResult.getKfTotalCount(),
                headcountResult.getGradeOneTotalCount(),
                headcountResult.getGradeTwoTotalCount(),
                headcountResult.getGradeThreeTotalCount(),
                headcountResult.getGradeFourTotalCount(),
                headcountResult.getGradeFiveTotalCount(),
                headcountResult.getGradeSixTotalCount(),
                headcountResult.getGradeSevenTotalCount(),
                headcountResult.getGradeEightTotalCount(),
                headcountResult.getGradeNineTotalCount(),
                headcountResult.getGradeTenTotalCount(),
                headcountResult.getGradeElevenTotalCount(),
                headcountResult.getGradeTwelveTotalCount(),
                headcountResult.getGradeEuTotalCount(),
                headcountResult.getGradeSuTotalCount(),
                headcountResult.getGradeHSCount(),
                headcountResult.getGradAdultCount(),
                headcountResult.getNonGradAdultCount(),

                headcountResult.getKhTotalFte(),
                headcountResult.getKfTotalFte(),
                headcountResult.getGradeOneTotalFte(),
                headcountResult.getGradeTwoTotalFte(),
                headcountResult.getGradeThreeTotalFte(),
                headcountResult.getGradeFourTotalFte(),
                headcountResult.getGradeFiveTotalFte(),
                headcountResult.getGradeSixTotalFte(),
                headcountResult.getGradeSevenTotalFte(),
                headcountResult.getGradeEightTotalFte(),
                headcountResult.getGradeNineTotalFte(),
                headcountResult.getGradeTenTotalFte(),
                headcountResult.getGradeElevenTotalFte(),
                headcountResult.getGradeTwelveTotalFte(),
                headcountResult.getGradeEuTotalFte(),
                headcountResult.getGradeSuTotalFte(),
                headcountResult.getGradAdultTotalFte(),
                headcountResult.getNonGradAdultTotalFte(),

                headcountResult.getKhLevelOneCount(),
                headcountResult.getKhLevelTwoCount(),
                headcountResult.getKhLevelThreeCount(),
                headcountResult.getKhEllCount(),
                headcountResult.getKhIndigenousCount(),
                headcountResult.getKhCoreFrenchCount(),
                headcountResult.getKhEarlyFrenchCount(),

                headcountResult.getKfLevelOneCount(),
                headcountResult.getKfLevelTwoCount(),
                headcountResult.getKfLevelThreeCount(),
                headcountResult.getKfEllCount(),
                headcountResult.getKfIndigenousCount(),
                headcountResult.getKfCoreFrenchCount(),
                headcountResult.getKfEarlyFrenchCount(),

                headcountResult.getGradeOneLevelOneCount(),
                headcountResult.getGradeOneLevelTwoCount(),
                headcountResult.getGradeOneLevelThreeCount(),
                headcountResult.getGradeOneEllCount(),
                headcountResult.getGradeOneIndigenousCount(),
                headcountResult.getGradeOneCoreFrenchCount(),
                headcountResult.getGradeOneEarlyFrenchCount(),

                headcountResult.getGradeTwoLevelOneCount(),
                headcountResult.getGradeTwoLevelTwoCount(),
                headcountResult.getGradeTwoLevelThreeCount(),
                headcountResult.getGradeTwoEllCount(),
                headcountResult.getGradeTwoIndigenousCount(),
                headcountResult.getGradeTwoCoreFrenchCount(),
                headcountResult.getGradeTwoEarlyFrenchCount(),

                headcountResult.getGradeThreeLevelOneCount(),
                headcountResult.getGradeThreeLevelTwoCount(),
                headcountResult.getGradeThreeLevelThreeCount(),
                headcountResult.getGradeThreeEllCount(),
                headcountResult.getGradeThreeIndigenousCount(),
                headcountResult.getGradeThreeCoreFrenchCount(),
                headcountResult.getGradeThreeEarlyFrenchCount(),

                headcountResult.getGradeFourLevelOneCount(),
                headcountResult.getGradeFourLevelTwoCount(),
                headcountResult.getGradeFourLevelThreeCount(),
                headcountResult.getGradeFourEllCount(),
                headcountResult.getGradeFourIndigenousCount(),
                headcountResult.getGradeFourCoreFrenchCount(),
                headcountResult.getGradeFourEarlyFrenchCount(),

                headcountResult.getGradeFiveLevelOneCount(),
                headcountResult.getGradeFiveLevelTwoCount(),
                headcountResult.getGradeFiveLevelThreeCount(),
                headcountResult.getGradeFiveEllCount(),
                headcountResult.getGradeFiveIndigenousCount(),
                headcountResult.getGradeFiveCoreFrenchCount(),
                headcountResult.getGradeFiveEarlyFrenchCount(),
                headcountResult.getGradeFiveLateFrenchCount(),

                headcountResult.getGradeSixLevelOneCount(),
                headcountResult.getGradeSixLevelTwoCount(),
                headcountResult.getGradeSixLevelThreeCount(),
                headcountResult.getGradeSixEllCount(),
                headcountResult.getGradeSixIndigenousCount(),
                headcountResult.getGradeSixCoreFrenchCount(),
                headcountResult.getGradeSixEarlyFrenchCount(),
                headcountResult.getGradeSixLateFrenchCount(),

                headcountResult.getGradeSevenLevelOneCount(),
                headcountResult.getGradeSevenLevelTwoCount(),
                headcountResult.getGradeSevenLevelThreeCount(),
                headcountResult.getGradeSevenEllCount(),
                headcountResult.getGradeSevenIndigenousCount(),
                headcountResult.getGradeSevenCoreFrenchCount(),
                headcountResult.getGradeSevenEarlyFrenchCount(),
                headcountResult.getGradeSevenLateFrenchCount(),

                headcountResult.getGradeEightLevelOneCount(),
                headcountResult.getGradeEightLevelTwoCount(),
                headcountResult.getGradeEightLevelThreeCount(),
                headcountResult.getGradeEightEllCount(),
                headcountResult.getGradeEightIndigenousCount(),
                headcountResult.getGradeEightCoreFrenchCount(),
                headcountResult.getGradeEightEarlyFrenchCount(),

                headcountResult.getGradeNineLevelOneCount(),
                headcountResult.getGradeNineLevelTwoCount(),
                headcountResult.getGradeNineLevelThreeCount(),
                headcountResult.getGradeNineEllCount(),
                headcountResult.getGradeNineIndigenousCount(),
                headcountResult.getGradeNineCoreFrenchCount(),
                headcountResult.getGradeNineEarlyFrenchCount(),

                headcountResult.getGradeTenLevelOneCount(),
                headcountResult.getGradeTenLevelTwoCount(),
                headcountResult.getGradeTenLevelThreeCount(),
                headcountResult.getGradeTenEllCount(),
                headcountResult.getGradeTenIndigenousCount(),
                headcountResult.getGradeTenCoreFrenchCount(),
                headcountResult.getGradeTenEarlyFrenchCount(),

                headcountResult.getGradeElevenLevelOneCount(),
                headcountResult.getGradeElevenLevelTwoCount(),
                headcountResult.getGradeElevenLevelThreeCount(),
                headcountResult.getGradeElevenEllCount(),
                headcountResult.getGradeElevenIndigenousCount(),
                headcountResult.getGradeElevenCoreFrenchCount(),
                headcountResult.getGradeElevenEarlyFrenchCount(),

                headcountResult.getGradeTwelveLevelOneCount(),
                headcountResult.getGradeTwelveLevelTwoCount(),
                headcountResult.getGradeTwelveLevelThreeCount(),
                headcountResult.getGradeTwelveEllCount(),
                headcountResult.getGradeTwelveIndigenousCount(),
                headcountResult.getGradeTwelveCoreFrenchCount(),
                headcountResult.getGradeTwelveEarlyFrenchCount(),

                headcountResult.getGradeEuLevelOneCount(),
                headcountResult.getGradeEuLevelTwoCount(),
                headcountResult.getGradeEuLevelThreeCount(),
                headcountResult.getGradeEuEllCount(),
                headcountResult.getGradeEuIndigenousCount(),
                headcountResult.getGradeEuCoreFrenchCount(),
                headcountResult.getGradeEuEarlyFrenchCount(),

                headcountResult.getGradeSuLevelOneCount(),
                headcountResult.getGradeSuLevelTwoCount(),
                headcountResult.getGradeSuLevelThreeCount(),
                headcountResult.getGradeSuEllCount(),
                headcountResult.getGradeSuIndigenousCount(),
                headcountResult.getGradeSuCoreFrenchCount(),
                headcountResult.getGradeSuEarlyFrenchCount(),

                headcountResult.getNonGradAdultLevelOneCount(),
                headcountResult.getNonGradAdultLevelTwoCount(),
                headcountResult.getNonGradAdultLevelThreeCount()
        ));
    }

    private List<String> prepareIndyAllDataForGraduatedCsv(IndyFundingGraduatedResult indyFundingResult, School school, District district, IndependentAuthority authority, CollectionEntity collection) {
        List<String> csvRowData = new ArrayList<>();
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        String groupKh;
        String groupKf;
        String group01;
        String group02;
        String group03;
        String group04;
        String group05;
        String group06;
        String group07;
        String groupEU;
        String group08;
        String group09;
        String group10;
        String group11;
        String group12;
        String groupSU;
        String groupGA;
        String groupHS;

        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID());
            groupKh = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }else{
            List<IndependentSchoolFundingGroup> schoolFundingGroups = school.getSchoolFundingGroups();
            groupKh = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }

        csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),

                groupKh,
                groupKf,
                group01,
                group02,
                group03,
                group04,
                group05,
                group06,
                group07,
                groupEU,
                group08,
                group09,
                group10,
                group11,
                group12,
                groupSU,
                groupGA,
                groupHS,

                indyFundingResult.getTotalCount(),
                indyFundingResult.getTotalFTE(),

                indyFundingResult.getKindHCount(),
                indyFundingResult.getKindFCount(),
                indyFundingResult.getGrade1Count(),
                indyFundingResult.getGrade2Count(),
                indyFundingResult.getGrade3Count(),
                indyFundingResult.getGrade4Count(),
                indyFundingResult.getGrade5Count(),
                indyFundingResult.getGrade6Count(),
                indyFundingResult.getGrade7Count(),
                indyFundingResult.getGradeEUCount(),
                indyFundingResult.getGrade8Count(),
                indyFundingResult.getGrade9Count(),
                indyFundingResult.getGrade10Count(),
                indyFundingResult.getGrade11Count(),
                indyFundingResult.getGrade12Count(),
                indyFundingResult.getGradeSUCount(),
                indyFundingResult.getGradeGACount(),
                indyFundingResult.getGradeHSCount(),

                indyFundingResult.getKindHFTE(),
                indyFundingResult.getKindFFTE(),
                indyFundingResult.getGrade1FTE(),
                indyFundingResult.getGrade2FTE(),
                indyFundingResult.getGrade3FTE(),
                indyFundingResult.getGrade4FTE(),
                indyFundingResult.getGrade5FTE(),
                indyFundingResult.getGrade6FTE(),
                indyFundingResult.getGrade7FTE(),
                indyFundingResult.getGradeEUFTE(),
                indyFundingResult.getGrade8FTE(),
                indyFundingResult.getGrade9FTE(),
                indyFundingResult.getGrade10FTE(),
                indyFundingResult.getGrade11FTE(),
                indyFundingResult.getGrade12FTE(),
                indyFundingResult.getGradeSUFTE(),
                indyFundingResult.getGradeGAFTE()
        ));
        return csvRowData;
    }

    private List<String> prepareIndyAllDataForCsv(IndyFundingResult indyFundingResult, School school, District district, IndependentAuthority authority, CollectionEntity collection, boolean isOnlineLearning) {
        List<String> csvRowData = new ArrayList<>();
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        String groupKh;
        String groupKf;
        String group01;
        String group02;
        String group03;
        String group04;
        String group05;
        String group06;
        String group07;
        String groupEU;
        String group08;
        String group09;
        String group10;
        String group11;
        String group12;
        String groupSU;
        String groupGA;
        String groupHS;

        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID());
            groupKh = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }else{
            List<IndependentSchoolFundingGroup> schoolFundingGroups = school.getSchoolFundingGroups();
            groupKh = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }

        csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),

                groupKh,
                groupKf,
                group01,
                group02,
                group03,
                group04,
                group05,
                group06,
                group07,
                groupEU,
                group08,
                group09,
                group10,
                group11,
                group12,
                groupSU,
                groupGA,
                groupHS,

                indyFundingResult.getTotalCountNoAdults(),
                indyFundingResult.getTotalFTENoAdults(),

                indyFundingResult.getKindHCountNoAdults(),
                indyFundingResult.getKindFCountNoAdults(),
                indyFundingResult.getGrade1CountNoAdults(),
                indyFundingResult.getGrade2CountNoAdults(),
                indyFundingResult.getGrade3CountNoAdults(),
                indyFundingResult.getGrade4CountNoAdults(),
                indyFundingResult.getGrade5CountNoAdults(),
                indyFundingResult.getGrade6CountNoAdults(),
                indyFundingResult.getGrade7CountNoAdults(),
                indyFundingResult.getGradeEUCountNoAdults(),
                indyFundingResult.getGrade8CountNoAdults(),
                indyFundingResult.getGrade9CountNoAdults(),
                indyFundingResult.getGrade10CountNoAdults(),
                indyFundingResult.getGrade11CountNoAdults(),
                indyFundingResult.getGrade12CountNoAdults(),
                indyFundingResult.getGradeSUCountNoAdults(),
                indyFundingResult.getGradeGACountNoAdults(),
                indyFundingResult.getGradeHSCountNoAdults(),

                indyFundingResult.getKindHFTENoAdults(),
                indyFundingResult.getKindFFTENoAdults(),
                indyFundingResult.getGrade1FTENoAdults(),
                indyFundingResult.getGrade2FTENoAdults(),
                indyFundingResult.getGrade3FTENoAdults(),
                indyFundingResult.getGrade4FTENoAdults(),
                indyFundingResult.getGrade5FTENoAdults(),
                indyFundingResult.getGrade6FTENoAdults(),
                indyFundingResult.getGrade7FTENoAdults(),
                indyFundingResult.getGradeEUFTENoAdults(),
                indyFundingResult.getGrade8FTENoAdults(),
                indyFundingResult.getGrade9FTENoAdults(),
                indyFundingResult.getGrade10FTENoAdults(),
                indyFundingResult.getGrade11FTENoAdults(),
                indyFundingResult.getGrade12FTENoAdults(),
                indyFundingResult.getGradeSUFTENoAdults(),
                indyFundingResult.getGradeGAFTENoAdults(),

                indyFundingResult.getTotalCountAdults(),
                indyFundingResult.getTotalFTEAdults(),

                indyFundingResult.getKindHCountAdults(),
                indyFundingResult.getKindFCountAdults(),
                indyFundingResult.getGrade1CountAdults(),
                indyFundingResult.getGrade2CountAdults(),
                indyFundingResult.getGrade3CountAdults(),
                indyFundingResult.getGrade4CountAdults(),
                indyFundingResult.getGrade5CountAdults(),
                indyFundingResult.getGrade6CountAdults(),
                indyFundingResult.getGrade7CountAdults(),
                indyFundingResult.getGradeEUCountAdults(),
                indyFundingResult.getGrade8CountAdults(),
                indyFundingResult.getGrade9CountAdults(),
                indyFundingResult.getGrade10CountAdults(),
                indyFundingResult.getGrade11CountAdults(),
                indyFundingResult.getGrade12CountAdults(),
                indyFundingResult.getGradeSUCountAdults(),
                indyFundingResult.getGradeGACountAdults(),
                indyFundingResult.getGradeHSCountAdults(),

                indyFundingResult.getKindHFTEAdults(),
                indyFundingResult.getKindFFTEAdults(),
                indyFundingResult.getGrade1FTEAdults(),
                indyFundingResult.getGrade2FTEAdults(),
                indyFundingResult.getGrade3FTEAdults(),
                indyFundingResult.getGrade4FTEAdults(),
                indyFundingResult.getGrade5FTEAdults(),
                indyFundingResult.getGrade6FTEAdults(),
                indyFundingResult.getGrade7FTEAdults(),
                indyFundingResult.getGradeEUFTEAdults(),
                indyFundingResult.getGrade8FTEAdults(),
                indyFundingResult.getGrade9FTEAdults(),
                indyFundingResult.getGrade10FTEAdults(),
                indyFundingResult.getGrade11FTEAdults(),
                indyFundingResult.getGrade12FTEAdults(),
                indyFundingResult.getGradeSUFTEAdults(),
                indyFundingResult.getGradeGAFTEAdults()
        ));

        if (isOnlineLearning) {
            csvRowData.addAll(Arrays.asList(
                    indyFundingResult.getTotalFTEKto9SA(),
                    indyFundingResult.getTotalFTE10to12SA()
            ));
        }

        return csvRowData;
    }

    private List<String> prepareIndyFundedDataForCsv(IndyFundingResult indyFundingResult, School school, District district, IndependentAuthority authority, CollectionEntity collection) {
        List<String> csvRowData = new ArrayList<>();
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        String groupKh;
        String groupKf;
        String group01;
        String group02;
        String group03;
        String group04;
        String group05;
        String group06;
        String group07;
        String groupEU;
        String group08;
        String group09;
        String group10;
        String group11;
        String group12;
        String groupSU;
        String groupGA;
        String groupHS;

        if(collection.getCollectionStatusCode().equalsIgnoreCase(CollectionStatus.COMPLETED.getCode())) {
            List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups = independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(UUID.fromString(school.getSchoolId()), collection.getCollectionID());
            groupKh = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupSnapshotForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }else{
            List<IndependentSchoolFundingGroup> schoolFundingGroups = school.getSchoolFundingGroups();
            groupKh = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDHALF.getTypeCode());
            groupKf = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.KINDFULL.getTypeCode());
            group01 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE01.getTypeCode());
            group02 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE02.getTypeCode());
            group03 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE03.getTypeCode());
            group04 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE04.getTypeCode());
            group05 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE05.getTypeCode());
            group06 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE06.getTypeCode());
            group07 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE07.getTypeCode());
            groupEU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.ELEMUNGR.getTypeCode());
            group08 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE08.getTypeCode());
            group09 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE09.getTypeCode());
            group10 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE10.getTypeCode());
            group11 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE11.getTypeCode());
            group12 = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADE12.getTypeCode());
            groupSU = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
            groupGA = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
            groupHS = TransformUtil.getFundingGroupForGrade(schoolFundingGroups, SchoolGradeCodes.HOMESCHOOL.getTypeCode());
        }

        var fteKhNoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupKh) ? indyFundingResult.getKindHFTENoAdults() : "0";
        var fteKfNoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupKf) ? indyFundingResult.getKindFFTENoAdults() : "0";
        var fte01NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group01) ? indyFundingResult.getGrade1FTENoAdults() : "0";
        var fte02NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group02) ? indyFundingResult.getGrade2FTENoAdults() : "0";
        var fte03NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group03) ? indyFundingResult.getGrade3FTENoAdults() : "0";
        var fte04NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group04) ? indyFundingResult.getGrade4FTENoAdults() : "0";
        var fte05NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group05) ? indyFundingResult.getGrade5FTENoAdults() : "0";
        var fte06NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group06) ? indyFundingResult.getGrade6FTENoAdults() : "0";
        var fte07NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group07) ? indyFundingResult.getGrade7FTENoAdults() : "0";
        var fteEUNoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupEU) ? indyFundingResult.getGradeEUFTENoAdults() : "0";
        var fte08NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group08) ? indyFundingResult.getGrade8FTENoAdults() : "0";
        var fte09NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group09) ? indyFundingResult.getGrade9FTENoAdults() : "0";
        var fte10NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group10) ? indyFundingResult.getGrade10FTENoAdults() : "0";
        var fte11NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group11) ? indyFundingResult.getGrade11FTENoAdults() : "0";
        var fte12NoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(group12) ? indyFundingResult.getGrade12FTENoAdults() : "0";
        var fteSUNoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupSU) ? indyFundingResult.getGradeSUFTENoAdults() : "0";
        var fteGANoAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupGA) ? indyFundingResult.getGradeGAFTENoAdults() : "0";

        var totalFTENoAdults = Double.parseDouble(fteKhNoAdults) +  Double.parseDouble(fteKfNoAdults) + Double.parseDouble(fte01NoAdults) + Double.parseDouble(fte02NoAdults)
                + Double.parseDouble(fte03NoAdults) + Double.parseDouble(fte04NoAdults) + Double.parseDouble(fte05NoAdults) + Double.parseDouble(fte06NoAdults) +
                Double.parseDouble(fte07NoAdults) + Double.parseDouble(fteEUNoAdults) + Double.parseDouble(fte08NoAdults) + Double.parseDouble(fte09NoAdults) +
                Double.parseDouble(fte10NoAdults) + Double.parseDouble(fte11NoAdults) + Double.parseDouble(fte12NoAdults) + Double.parseDouble(fteSUNoAdults)
                + Double.parseDouble(fteGANoAdults);

        var fteKhAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupKh) ? indyFundingResult.getKindHFTEAdults() : "0";
        var fteKfAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupKf) ? indyFundingResult.getKindFFTEAdults() : "0";
        var fte01Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group01) ? indyFundingResult.getGrade1FTEAdults() : "0";
        var fte02Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group02) ? indyFundingResult.getGrade2FTEAdults() : "0";
        var fte03Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group03) ? indyFundingResult.getGrade3FTEAdults() : "0";
        var fte04Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group04) ? indyFundingResult.getGrade4FTEAdults() : "0";
        var fte05Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group05) ? indyFundingResult.getGrade5FTEAdults() : "0";
        var fte06Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group06) ? indyFundingResult.getGrade6FTEAdults() : "0";
        var fte07Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group07) ? indyFundingResult.getGrade7FTEAdults() : "0";
        var fteEUAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupEU) ? indyFundingResult.getGradeEUFTEAdults() : "0";
        var fte08Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group08) ? indyFundingResult.getGrade8FTEAdults() : "0";
        var fte09Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group09) ? indyFundingResult.getGrade9FTEAdults() : "0";
        var fte10Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group10) ? indyFundingResult.getGrade10FTEAdults() : "0";
        var fte11Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group11) ? indyFundingResult.getGrade11FTEAdults() : "0";
        var fte12Adults = TransformUtil.isSchoolFundingGroup1orGroup2(group12) ? indyFundingResult.getGrade12FTEAdults() : "0";
        var fteSUAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupSU) ? indyFundingResult.getGradeSUFTEAdults() : "0";
        var fteGAAdults = TransformUtil.isSchoolFundingGroup1orGroup2(groupGA) ? indyFundingResult.getGradeGAFTEAdults() : "0";

        var totalFTEAdults = Double.parseDouble(fteKhAdults) +  Double.parseDouble(fteKfAdults) + Double.parseDouble(fte01Adults) + Double.parseDouble(fte02Adults)
                + Double.parseDouble(fte03Adults) + Double.parseDouble(fte04Adults) + Double.parseDouble(fte05Adults) + Double.parseDouble(fte06Adults) +
                Double.parseDouble(fte07Adults) + Double.parseDouble(fteEUAdults) + Double.parseDouble(fte08Adults) + Double.parseDouble(fte09Adults) +
                Double.parseDouble(fte10Adults) + Double.parseDouble(fte11Adults) + Double.parseDouble(fte12Adults) + Double.parseDouble(fteSUAdults)
                + Double.parseDouble(fteGAAdults);

                csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),
                collection.getSnapshotDate().format(DateTimeFormatter.ofPattern(REPORT_DATE_FORMAT)),

                groupKh,
                groupKf,
                group01,
                group02,
                group03,
                group04,
                group05,
                group06,
                group07,
                groupEU,
                group08,
                group09,
                group10,
                group11,
                group12,
                groupSU,
                groupGA,
                groupHS,

                indyFundingResult.getTotalCountNoAdults(),
                Double.toString(totalFTENoAdults),

                indyFundingResult.getKindHCountNoAdults(),
                indyFundingResult.getKindFCountNoAdults(),
                indyFundingResult.getGrade1CountNoAdults(),
                indyFundingResult.getGrade2CountNoAdults(),
                indyFundingResult.getGrade3CountNoAdults(),
                indyFundingResult.getGrade4CountNoAdults(),
                indyFundingResult.getGrade5CountNoAdults(),
                indyFundingResult.getGrade6CountNoAdults(),
                indyFundingResult.getGrade7CountNoAdults(),
                indyFundingResult.getGradeEUCountNoAdults(),
                indyFundingResult.getGrade8CountNoAdults(),
                indyFundingResult.getGrade9CountNoAdults(),
                indyFundingResult.getGrade10CountNoAdults(),
                indyFundingResult.getGrade11CountNoAdults(),
                indyFundingResult.getGrade12CountNoAdults(),
                indyFundingResult.getGradeSUCountNoAdults(),
                indyFundingResult.getGradeGACountNoAdults(),
                indyFundingResult.getGradeHSCountNoAdults(),

                fteKhNoAdults,
                fteKfNoAdults,
                fte01NoAdults,
                fte02NoAdults,
                fte03NoAdults,
                fte04NoAdults,
                fte05NoAdults,
                fte06NoAdults,
                fte07NoAdults,
                fteEUNoAdults,
                fte08NoAdults,
                fte09NoAdults,
                fte10NoAdults,
                fte11NoAdults,
                fte12NoAdults,
                fteSUNoAdults,
                fteGANoAdults,

                indyFundingResult.getTotalCountAdults(),
                Double.toString(totalFTEAdults),

                indyFundingResult.getKindHCountAdults(),
                indyFundingResult.getKindFCountAdults(),
                indyFundingResult.getGrade1CountAdults(),
                indyFundingResult.getGrade2CountAdults(),
                indyFundingResult.getGrade3CountAdults(),
                indyFundingResult.getGrade4CountAdults(),
                indyFundingResult.getGrade5CountAdults(),
                indyFundingResult.getGrade6CountAdults(),
                indyFundingResult.getGrade7CountAdults(),
                indyFundingResult.getGradeEUCountAdults(),
                indyFundingResult.getGrade8CountAdults(),
                indyFundingResult.getGrade9CountAdults(),
                indyFundingResult.getGrade10CountAdults(),
                indyFundingResult.getGrade11CountAdults(),
                indyFundingResult.getGrade12CountAdults(),
                indyFundingResult.getGradeSUCountAdults(),
                indyFundingResult.getGradeGACountAdults(),
                indyFundingResult.getGradeHSCountAdults(),

                fteKhAdults,
                fteKfAdults,
                fte01Adults,
                fte02Adults,
                fte03Adults,
                fte04Adults,
                fte05Adults,
                fte06Adults,
                fte07Adults,
                fteEUAdults,
                fte08Adults,
                fte09Adults,
                fte10Adults,
                fte11Adults,
                fte12Adults,
                fteSUAdults,
                fteGAAdults
        ));
        return csvRowData;
    }

    private List<String> prepareRefugeeEnrolmentFteData(EnrolmentHeadcountFteResult headcountResult, SchoolTombstone school) {
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        return new ArrayList<>(Arrays.asList(
                school.getMincode().substring(0, 3),
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),

                headcountResult.getKhRefugeeCount(),
                headcountResult.getKhEllCount(),
                headcountResult.getKfRefugeeCount(),
                headcountResult.getKfEllCount(),
                headcountResult.getGradeOneRefugeeCount(),
                headcountResult.getGradeOneEllCount(),
                headcountResult.getGradeTwoRefugeeCount(),
                headcountResult.getGradeTwoEllCount(),
                headcountResult.getGradeThreeRefugeeCount(),
                headcountResult.getGradeThreeEllCount(),
                headcountResult.getGradeFourRefugeeCount(),
                headcountResult.getGradeFourEllCount(),
                headcountResult.getGradeFiveRefugeeCount(),
                headcountResult.getGradeFiveEllCount(),
                headcountResult.getGradeSixRefugeeCount(),
                headcountResult.getGradeSixEllCount(),
                headcountResult.getGradeSevenRefugeeCount(),
                headcountResult.getGradeSevenEllCount(),
                headcountResult.getGradeEightRefugeeCount(),
                headcountResult.getGradeEightEllCount(),
                headcountResult.getGradeNineRefugeeCount(),
                headcountResult.getGradeNineEllCount(),
                headcountResult.getGradeTenRefugeeCount(),
                headcountResult.getGradeTenEllCount(),
                headcountResult.getGradeElevenRefugeeCount(),
                headcountResult.getGradeElevenEllCount(),
                headcountResult.getGradeTwelveRefugeeCount(),
                headcountResult.getGradeTwelveEllCount(),
                headcountResult.getGradeEuRefugeeCount(),
                headcountResult.getGradeEuEllCount(),
                headcountResult.getGradeSuRefugeeCount(),
                headcountResult.getGradeSuEllCount(),

                headcountResult.getKhRefugeeTotalFte(),
                headcountResult.getKfRefugeeTotalFte(),
                headcountResult.getGradeOneRefugeeTotalFte(),
                headcountResult.getGradeTwoRefugeeTotalFte(),
                headcountResult.getGradeThreeRefugeeTotalFte(),
                headcountResult.getGradeFourRefugeeTotalFte(),
                headcountResult.getGradeFiveRefugeeTotalFte(),
                headcountResult.getGradeSixRefugeeTotalFte(),
                headcountResult.getGradeSevenRefugeeTotalFte(),
                headcountResult.getGradeEightRefugeeTotalFte(),
                headcountResult.getGradeNineRefugeeTotalFte(),
                headcountResult.getGradeTenRefugeeTotalFte(),
                headcountResult.getGradeElevenRefugeeTotalFte(),
                headcountResult.getGradeTwelveRefugeeTotalFte(),
                headcountResult.getGradeEuRefugeeTotalFte(),
                headcountResult.getGradeSuRefugeeTotalFte()
        ));
    }
}
