package ca.bc.gov.educ.studentdatacollection.api.service.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolCategoryCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.*;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDuplicateRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentSearchService;
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
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.MinistryReportTypeCode.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndyFundingReportHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySchoolEnrolmentHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.DISTRICT_NUMBER;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.SCHOOL_NAME;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.IndySpecialEducationFundingHeadcountHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports.SchoolEnrolmentHeader.*;
import static ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil.flagCountIfNoSchoolFundingGroup;
import static org.springframework.http.HttpStatus.BAD_REQUEST;


@Service
@Slf4j
@RequiredArgsConstructor
public class CSVReportService {
    private final SdcDuplicateRepository sdcDuplicateRepository;
    private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
    private final CollectionRepository collectionRepository;
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;
    private final SdcSchoolCollectionStudentSearchService sdcSchoolCollectionStudentSearchService;
    private final RestUtils restUtils;
    private final ValidationRulesService validationService;
    private static final String COLLECTION_ID = "collectionID";
    private static final String SCHOOL_ID = "schoolID";
    private static final String INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB and SEPT collections";
    private static final String HEADCOUNTS_INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB and MAY collections";
    private static final String REFUGEE_HEADCOUNTS_INVALID_COLLECTION_TYPE = "Invalid collectionType. Report can only be generated for FEB collection";

    // Independent School Funding Report - Standard Student report AND Independent School Funding Report - Online Learning report AND Independent School Funding Report - Non Graduated Adult report
    public DownloadableReportResponse generateIndyFundingReport(UUID collectionID, boolean isOnlineLearning, boolean isNonGraduatedAdult, boolean isFundedReport) {
        List<IndyFundingResult> results;
        // if it's non-graduated adult report variant use query for non-graduated adults
        if (isNonGraduatedAdult){
            results = sdcSchoolCollectionStudentRepository.getIndyFundingHeadcountsNonGraduatedAdultByCollectionId(collectionID);
        } else {
            results = sdcSchoolCollectionStudentRepository.getIndyFundingHeadcountsByCollectionId(collectionID);
        }
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(DISTRICT_NUMBER.getCode(), IndyFundingReportHeader.DISTRICT_NAME.getCode(), IndyFundingReportHeader.AUTHORITY_NUMBER.getCode(), IndyFundingReportHeader.AUTHORITY_NAME.getCode(), IndyFundingReportHeader.SCHOOL_NUMBER.getCode(),
                        SCHOOL_NAME.getCode(), FUNDING_GROUP.getCode(),

                        KIND_HT_FUNDING_GROUP.getCode(), KIND_FT_FUNDING_GROUP.getCode(), GRADE_01_FUNDING_GROUP.getCode(),
                        GRADE_02_FUNDING_GROUP.getCode(), GRADE_03_FUNDING_GROUP.getCode(), GRADE_04_FUNDING_GROUP.getCode(), GRADE_05_FUNDING_GROUP.getCode(),GRADE_06_FUNDING_GROUP.getCode(),
                        GRADE_07_FUNDING_GROUP.getCode(), GRADE_EU_FUNDING_GROUP.getCode(), GRADE_08_FUNDING_GROUP.getCode(),GRADE_09_FUNDING_GROUP.getCode(), GRADE_10_FUNDING_GROUP.getCode(),
                        GRADE_11_FUNDING_GROUP.getCode(),GRADE_12_FUNDING_GROUP.getCode(), GRADE_SU_FUNDING_GROUP.getCode(), GRADE_GA_FUNDING_GROUP.getCode(), GRADE_HS_FUNDING_GROUP.getCode(),

                        TOTAL_HEADCOUNT.getCode(), TOTAL_FTE.getCode(),

                        KIND_HT_HEADCOUNT.getCode(), KIND_FT_HEADCOUNT.getCode(), GRADE_01_HEADCOUNT.getCode(), GRADE_02_HEADCOUNT.getCode(), GRADE_03_HEADCOUNT.getCode(),GRADE_04_HEADCOUNT.getCode(),
                        GRADE_05_HEADCOUNT.getCode(), GRADE_06_HEADCOUNT.getCode(),GRADE_07_HEADCOUNT.getCode(), GRADE_EU_HEADCOUNT.getCode(), GRADE_08_HEADCOUNT.getCode(),GRADE_09_HEADCOUNT.getCode(),
                        GRADE_10_HEADCOUNT.getCode(), GRADE_11_HEADCOUNT.getCode(),GRADE_12_HEADCOUNT.getCode(), GRADE_SU_HEADCOUNT.getCode(), GRAD_ADULT_HEADCOUNT.getCode(), GRADE_HS_HEADCOUNT.getCode(),

                        KIND_HT_FTE_COUNT.getCode(), KIND_FT_FTE_COUNT.getCode(), GRADE_ONE_FTE_COUNT.getCode(), GRADE_TWO_FTE_COUNT.getCode(), GRADE_THREE_FTE_COUNT.getCode(), GRADE_FOUR_FTE_COUNT.getCode(),
                        GRADE_FIVE_FTE_COUNT.getCode(), GRADE_SIX_FTE_COUNT.getCode(),GRADE_SEVEN_FTE_COUNT.getCode(), EU_FTE_COUNT.getCode(), GRADE_EIGHT_FTE_COUNT.getCode(),GRADE_NINE_FTE_COUNT.getCode(),
                        GRADE_TEN_FTE_COUNT.getCode(), GRADE_ELEVEN_FTE_COUNT.getCode(), GRADE_TWELVE_FTE_COUNT.getCode(), SU_FTE_COUNT.getCode(), GA_FTE_COUNT.getCode(), HS_FTE_COUNT.getCode())
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
                                csvRowData = prepareIndyFundedDataForCsv(result, school, district, authority);
                            }else{
                                csvRowData = prepareIndyAllDataForCsv(result, school, district, authority);
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
            }else if (isNonGraduatedAdult) {
                downloadableReport.setReportType(NON_GRADUATED_ADULT_INDY_FUNDING_REPORT.getCode());
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
                            district = restUtils.getDistrictByDistrictID(school.getDistrictId()).orElseThrow(() -> new EntityNotFoundException(District.class, "districtID", school.getDistrictId()));
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

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(SCHOOL.getCode(), KIND_HT.getCode(), KIND_FT.getCode(),GRADE_01.getCode(), GRADE_02.getCode(), GRADE_03.getCode(), GRADE_04.getCode(),
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
        List<SdcSchoolCollectionStudentEntity> students =
                sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_CollectionEntity_CollectionIDAndEnrolledGradeCodeIn(collectionID, grades);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaFebRegistrationHeader.MINCODE.getCode(), FsaFebRegistrationHeader.STUDENT_PEN.getCode(),
                FsaFebRegistrationHeader.NEXT_YEAR_GRADE.getCode(), FsaFebRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaFebRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentEntity student : students) {
                var schoolOpt = restUtils.getSchoolBySchoolID(String.valueOf(student.getSdcSchoolCollection().getSchoolID()));
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
        List<SdcSchoolCollectionStudentEntity> students =
                sdcSchoolCollectionStudentRepository.findAllBySdcSchoolCollection_CollectionEntity_CollectionIDAndEnrolledGradeCodeIn(collectionID, grades);
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader(
                FsaSeptRegistrationHeader.MINCODE.getCode(), FsaSeptRegistrationHeader.STUDENT_PEN.getCode(),
                FsaSeptRegistrationHeader.ENROLLED_GRADE.getCode(), FsaSeptRegistrationHeader.LEGAL_FIRST_NAME.getCode(),
                FsaSeptRegistrationHeader.LEGAL_LAST_NAME.getCode()
        ).build();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (SdcSchoolCollectionStudentEntity student : students) {
                var schoolOpt = restUtils.getSchoolBySchoolID(String.valueOf(student.getSdcSchoolCollection().getSchoolID()));
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
        List<IndySpecialEdAdultHeadcountResult> results = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryForIndiesAndOffshoreByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);
        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(
                        IndySpecialEducationHeadcountHeader.AUTHORITY_NUMBER.getCode(), IndySpecialEducationHeadcountHeader.AUTHORITY_NAME.getCode(),
                        IndySpecialEducationHeadcountHeader.MIN_CODE.getCode(),
                        IndySpecialEducationHeadcountHeader.SCHOOL.getCode(),
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
                        List<String> csvRowData = prepareIndyInclusiveEdDataForCsv(result, school, authority);
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
        List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsByCollectionId(collectionID);
        var mappedSeptData = getLastSeptCollectionSchoolMap(collectionID);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(DISTRICT_NUMBER.getCode(), IndySpecialEducationFundingHeadcountHeader.DISTRICT_NAME.getCode(), IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NUMBER.getCode(), IndySpecialEducationFundingHeadcountHeader.AUTHORITY_NAME.getCode(), MINCODE.getCode(), SCHOOL_NAME.getCode(),
                        POSITIVE_CHANGE_LEVEL_1.getCode(),POSITIVE_CHANGE_LEVEL_2.getCode(),
                        POSITIVE_CHANGE_LEVEL_3.getCode(), NET_CHANGE_LEVEL_1.getCode(), NET_CHANGE_LEVEL_2.getCode(), NET_CHANGE_LEVEL_3.getCode(),SEPT_LEVEL_1.getCode(), SEPT_LEVEL_2.getCode(),
                        SEPT_LEVEL_3.getCode(),FEB_LEVEL_1.getCode(),FEB_LEVEL_2.getCode(),FEB_LEVEL_3.getCode())
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
                        var netChangeLevel1 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : "0", februaryCollectionRecord.getLevelOnes());
                        var netChangeLevel2 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : "0", februaryCollectionRecord.getLevelTwos());
                        var netChangeLevel3 = TransformUtil.getNetChange(septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : "0", februaryCollectionRecord.getLevelThrees());
                        List<String> csvRowData = prepareIndyInclusiveEdFundingDataForCsv(septCollectionRecord, februaryCollectionRecord, school,
                                authority, district, positiveChangeLevel1, positiveChangeLevel2, positiveChangeLevel3, netChangeLevel1, netChangeLevel2, netChangeLevel3);
                        csvPrinter.printRecord(csvRowData);
                        if (septCollectionRecord != null) {
                            fundingReportTotals.setTotSeptLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel1s(), septCollectionRecord.getLevelOnes()));
                            fundingReportTotals.setTotSeptLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel2s(), septCollectionRecord.getLevelTwos()));
                            fundingReportTotals.setTotSeptLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotSeptLevel3s(), septCollectionRecord.getLevelThrees()));
                        }
                        fundingReportTotals.setTotFebLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel1s(), februaryCollectionRecord.getLevelOnes()));
                        fundingReportTotals.setTotFebLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel2s(), februaryCollectionRecord.getLevelTwos()));
                        fundingReportTotals.setTotFebLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotFebLevel3s(), februaryCollectionRecord.getLevelThrees()));
                        fundingReportTotals.setTotPositiveChangeLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel1s(), positiveChangeLevel1));
                        fundingReportTotals.setTotPositiveChangeLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel2s(), positiveChangeLevel2));
                        fundingReportTotals.setTotPositiveChangeLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotPositiveChangeLevel3s(), positiveChangeLevel3));
                        fundingReportTotals.setTotNetLevel1s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel1s(), netChangeLevel1));
                        fundingReportTotals.setTotNetLevel2s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel2s(), netChangeLevel2));
                        fundingReportTotals.setTotNetLevel3s(TransformUtil.addValueIfExists(fundingReportTotals.getTotNetLevel3s(), netChangeLevel3));
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
                Integer.toString(totals.getTotPositiveChangeLevel1s()),
                Integer.toString(totals.getTotPositiveChangeLevel2s()),
                Integer.toString(totals.getTotPositiveChangeLevel3s()),
                Integer.toString(totals.getTotNetLevel1s()),
                Integer.toString(totals.getTotNetLevel2s()),
                Integer.toString(totals.getTotNetLevel3s()),
                Integer.toString(totals.getTotSeptLevel1s()),
                Integer.toString(totals.getTotSeptLevel2s()),
                Integer.toString(totals.getTotSeptLevel3s()),
                Integer.toString(totals.getTotFebLevel1s()),
                Integer.toString(totals.getTotFebLevel2s()),
                Integer.toString(totals.getTotFebLevel3s())
        ));
        return csvRowData;
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

    private Map<String, SpecialEdHeadcountResult> getLastSeptCollectionSchoolMap(UUID collectionID){
        var lastSeptCollectionOpt = sdcSchoolCollectionRepository.findLastCollectionByType(CollectionTypeCodes.SEPTEMBER.getTypeCode(), collectionID);
        if(lastSeptCollectionOpt.isEmpty()) {
            throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
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
                                                                 String netChangeLevel1,
                                                                 String netChangeLevel2,
                                                                 String netChangeLevel3) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getMincode(),
                school.getDisplayName(),
                positiveChangeLevel1,
                positiveChangeLevel2,
                positiveChangeLevel3,
                netChangeLevel1,
                netChangeLevel2,
                netChangeLevel3,
                septCollectionRecord != null ? septCollectionRecord.getLevelOnes() : "0",
                septCollectionRecord != null ? septCollectionRecord.getLevelTwos() : "0",
                septCollectionRecord != null ? septCollectionRecord.getLevelThrees() : "0",
                februaryCollectionRecord.getLevelOnes(),
                februaryCollectionRecord.getLevelTwos(),
                februaryCollectionRecord.getLevelThrees()
        ));
        return csvRowData;
    }

    private List<String> prepareIndyInclusiveEdDataForCsv(IndySpecialEdAdultHeadcountResult result, School school, IndependentAuthority authority) {
        List<String> csvRowData = new ArrayList<>();

        csvRowData.addAll(Arrays.asList(
                authority != null ? authority.getAuthorityNumber() : "",
                authority != null ? authority.getDisplayName() : "",
                school.getMincode(),
                school.getDisplayName(),
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

    private List<String> prepareFsaDataForCsv(SdcSchoolCollectionStudentEntity student, String studentGrade, SchoolTombstone school) {

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

    // Enroled Headcounts and FTEs by School report
    public DownloadableReportResponse generateEnrolledHeadcountsAndFteReport(UUID collectionID) {
        List<EnrolmentHeadcountFteResult> results = sdcSchoolCollectionStudentRepository.getEnrolmentHeadcountsAndFteByCollectionId(collectionID);
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
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

    // Enroled Headcounts and FTEs For CE and OL Schools report
    public DownloadableReportResponse generateEnrolmentHeadcountsAndFteReportForCEAndOLSchools(UUID collectionID) {
        var collectionOpt = collectionRepository.findById(collectionID);

        if(collectionOpt.isEmpty()){
            throw new EntityNotFoundException(Collection.class, COLLECTION_ID, collectionID.toString());
        }

        CollectionEntity collection = collectionOpt.get();
        if(!collection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.MAY.getTypeCode()) && !collection.getCollectionTypeCode().equalsIgnoreCase(CollectionTypeCodes.FEBRUARY.getTypeCode())) {
            throw new InvalidPayloadException(createError(HEADCOUNTS_INVALID_COLLECTION_TYPE));
        }

        List<EnrolmentHeadcountFteResult> results = sdcSchoolCollectionStudentRepository.getEnrolmentHeadcountsAndFteWithRefugeeByCollectionId(collectionID);
        var mappedSeptData = getEnrolmentHeadcountFteResultForLastSeptCollection(collectionID);

        List<String> headers = Arrays.stream(CEAndOLEnrolmentAndFteHeader.values()).map(CEAndOLEnrolmentAndFteHeader::getCode).toList();
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .build();
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(byteArrayOutputStream));
            CSVPrinter csvPrinter = new CSVPrinter(writer, csvFormat);

            for (EnrolmentHeadcountFteResult result : results) {
                var septCollectionRecord = mappedSeptData.get(result.getSchoolID());

                var schoolOpt = restUtils.getSchoolBySchoolID(result.getSchoolID());
                if(schoolOpt.isPresent() &&
                        (schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.CONT_ED.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DISTONLINE.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()))) {
                    List<String> csvRowData = prepareEnrolmentFteDataForCEAndOLSchools(result, septCollectionRecord, schoolOpt.get());
                    csvPrinter.printRecord(csvRowData);
                }
            }
            csvPrinter.flush();

            var downloadableReport = new DownloadableReportResponse();
            downloadableReport.setReportType(ENROLMENT_HEADCOUNTS_AND_FTE_REPORT_FOR_OL_AND_CE_SCHOOLS.getCode());
            downloadableReport.setDocumentData(Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray()));

            return downloadableReport;
        } catch (IOException e) {
            throw new StudentDataCollectionAPIRuntimeException(e);
        }
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
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.ALT_PROGS.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.YOUTH.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.SHORT_PRP.getCode()) ||
                                schoolOpt.get().getFacilityTypeCode().equalsIgnoreCase(FacilityTypeCodes.LONG_PRP.getCode()))) {
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

    private Map<String, EnrolmentHeadcountFteResult> getEnrolmentHeadcountFteResultForLastSeptCollection(UUID collectionID){
        var lastSeptCollectionOpt = sdcSchoolCollectionRepository.findLastCollectionByType(CollectionTypeCodes.SEPTEMBER.getTypeCode(), collectionID);
        if(lastSeptCollectionOpt.isEmpty()) {
            throw new EntityNotFoundException(CollectionEntity.class, COLLECTION_ID, collectionID.toString());
        }
        List<EnrolmentHeadcountFteResult> lastSeptCollectionRawData = sdcSchoolCollectionStudentRepository.getEnrolmentHeadcountsAndFteWithRefugeeByCollectionId(lastSeptCollectionOpt.get().getCollectionID());
        return lastSeptCollectionRawData.stream().collect(Collectors.toMap(EnrolmentHeadcountFteResult::getSchoolID, item -> item));
    }

    private ApiError createError(String message) {
        return ApiError.builder().timestamp(LocalDateTime.now()).message(message).status(BAD_REQUEST).build();
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

    private List<String> prepareEnrolmentFteDataForCEAndOLSchools(EnrolmentHeadcountFteResult headcountResult, EnrolmentHeadcountFteResult septHeadcountResult, SchoolTombstone school) {
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());
        return new ArrayList<>(Arrays.asList(
                school.getMincode().substring(0, 3),
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getKhTotalCount() : "0", headcountResult.getKhTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getKhRefugeeCount() : "0", headcountResult.getKhRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getKhEllCount() : "0", headcountResult.getKhEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeOneTotalCount() : "0", headcountResult.getGradeOneTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeOneRefugeeCount() : "0", headcountResult.getGradeOneRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeOneEllCount() : "0", headcountResult.getGradeOneEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwoTotalCount() : "0", headcountResult.getGradeTwoTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwoRefugeeCount() : "0", headcountResult.getGradeTwoRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwoEllCount() : "0", headcountResult.getGradeTwoEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeThreeTotalCount() : "0", headcountResult.getGradeThreeTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeThreeRefugeeCount() : "0", headcountResult.getGradeThreeRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeThreeEllCount() : "0", headcountResult.getGradeThreeEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFourTotalCount() : "0", headcountResult.getGradeFourTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFourRefugeeCount() : "0", headcountResult.getGradeFourRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFourEllCount() : "0", headcountResult.getGradeFourEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFiveTotalCount() : "0", headcountResult.getGradeFiveTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFiveRefugeeCount() : "0", headcountResult.getGradeFiveRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFiveEllCount() : "0", headcountResult.getGradeFiveEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSixTotalCount() : "0", headcountResult.getGradeSixTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSixRefugeeCount() : "0", headcountResult.getGradeSixRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSixEllCount() : "0", headcountResult.getGradeSixEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSevenTotalCount() : "0", headcountResult.getGradeSevenTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSevenRefugeeCount() : "0", headcountResult.getGradeSevenRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSevenEllCount() : "0", headcountResult.getGradeSevenEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEightTotalCount() : "0", headcountResult.getGradeEightTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEightRefugeeCount() : "0", headcountResult.getGradeEightRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEightEllCount() : "0", headcountResult.getGradeEightEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeNineTotalCount() : "0", headcountResult.getGradeNineTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeNineRefugeeCount() : "0", headcountResult.getGradeNineRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeNineEllCount() : "0", headcountResult.getGradeNineEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTenTotalCount() : "0", headcountResult.getGradeTenTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTenRefugeeCount() : "0", headcountResult.getGradeTenRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTenEllCount() : "0", headcountResult.getGradeTenEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeElevenTotalCount() : "0", headcountResult.getGradeElevenTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeElevenRefugeeCount() : "0", headcountResult.getGradeElevenRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeElevenEllCount() : "0", headcountResult.getGradeElevenEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwelveTotalCount() : "0", headcountResult.getGradeTwelveTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwelveRefugeeCount() : "0", headcountResult.getGradeTwelveRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwelveEllCount() : "0", headcountResult.getGradeTwelveEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEuTotalCount() : "0", headcountResult.getGradeEuTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEuRefugeeCount() : "0", headcountResult.getGradeEuRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEuEllCount() : "0", headcountResult.getGradeEuEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSuTotalCount() : "0", headcountResult.getGradeSuTotalCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSuRefugeeCount() : "0", headcountResult.getGradeSuRefugeeCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSuEllCount() : "0", headcountResult.getGradeSuEllCount()),

                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradAdultCount() : "0", headcountResult.getGradAdultCount()),
                TransformUtil.getPositiveChange(septHeadcountResult != null ? septHeadcountResult.getNonGradAdultCount() : "0", headcountResult.getNonGradAdultCount()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getKhTotalFte() : "0", headcountResult.getKhTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getKhRefugeeTotalFte() : "0", headcountResult.getKhRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeOneTotalFte() : "0", headcountResult.getGradeOneTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeOneRefugeeTotalFte() : "0", headcountResult.getGradeOneRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwoTotalFte() : "0", headcountResult.getGradeTwoTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwoRefugeeTotalFte() : "0", headcountResult.getGradeTwoRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeThreeTotalFte() : "0", headcountResult.getGradeThreeTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeThreeRefugeeTotalFte() : "0", headcountResult.getGradeThreeRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFourTotalFte() : "0", headcountResult.getGradeFourTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFourRefugeeTotalFte() : "0", headcountResult.getGradeFourRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFiveTotalFte() : "0", headcountResult.getGradeFiveTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeFiveRefugeeTotalFte() : "0", headcountResult.getGradeFiveRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSixTotalFte() : "0", headcountResult.getGradeSixTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSixRefugeeTotalFte() : "0", headcountResult.getGradeSixRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSevenTotalFte() : "0", headcountResult.getGradeSevenTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSevenRefugeeTotalFte() : "0", headcountResult.getGradeSevenRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEightTotalFte() : "0", headcountResult.getGradeEightTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEightRefugeeTotalFte() : "0", headcountResult.getGradeEightRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeNineTotalFte() : "0", headcountResult.getGradeNineTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeNineRefugeeTotalFte() : "0", headcountResult.getGradeNineRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTenTotalFte() : "0", headcountResult.getGradeTenTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTenRefugeeTotalFte() : "0", headcountResult.getGradeTenRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeElevenTotalFte() : "0", headcountResult.getGradeElevenTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeElevenRefugeeTotalFte() : "0", headcountResult.getGradeElevenRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwelveTotalFte() : "0", headcountResult.getGradeTwelveTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeTwelveRefugeeTotalFte() : "0", headcountResult.getGradeTwelveRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEuTotalFte() : "0", headcountResult.getGradeEuTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeEuRefugeeTotalFte() : "0", headcountResult.getGradeEuRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSuTotalFte() : "0", headcountResult.getGradeSuTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradeSuRefugeeTotalFte() : "0", headcountResult.getGradeSuRefugeeTotalFte()),

                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getGradAdultTotalFte() : "0", headcountResult.getGradAdultTotalFte()),
                TransformUtil.getFTEPositiveChange(septHeadcountResult != null ? septHeadcountResult.getNonGradAdultTotalFte() : "0", headcountResult.getNonGradAdultTotalFte())
        ));
    }

    private List<String> prepareIndyAllDataForCsv(IndyFundingResult indyFundingResult, School school, District district, IndependentAuthority authority) {
        List<String> csvRowData = new ArrayList<>();
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),

                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.KINDHALF.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.KINDFULL.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE01.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE02.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE03.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE04.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE05.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE06.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE07.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.ELEMUNGR.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE08.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE09.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE10.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE11.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE12.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode()),
                TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.HOMESCHOOL.getTypeCode()),

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
                indyFundingResult.getGradeGAFTE(),
                indyFundingResult.getGradeHSFTE()
        ));
        return csvRowData;
    }

    private List<String> prepareIndyFundedDataForCsv(IndyFundingResult indyFundingResult, School school, District district, IndependentAuthority authority) {
        List<String> csvRowData = new ArrayList<>();
        var facilityType = restUtils.getFacilityTypeCode(school.getFacilityTypeCode());

        var groupKh = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.KINDHALF.getTypeCode());
        var groupKf = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.KINDFULL.getTypeCode());
        var group01 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE01.getTypeCode());
        var group02 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE02.getTypeCode());
        var group03 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE03.getTypeCode());
        var group04 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE04.getTypeCode());
        var group05 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE05.getTypeCode());
        var group06 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE06.getTypeCode());
        var group07 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE07.getTypeCode());
        var groupEU = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.ELEMUNGR.getTypeCode());
        var group08 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE08.getTypeCode());
        var group09 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE09.getTypeCode());
        var group10 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE10.getTypeCode());
        var group11 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE11.getTypeCode());
        var group12 = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADE12.getTypeCode());
        var groupSU = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.SECONDARY_UNGRADED.getTypeCode());
        var groupGA = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.GRADUATED_ADULT.getTypeCode());
        var groupHS = TransformUtil.getFundingGroupForGrade(school, SchoolGradeCodes.HOMESCHOOL.getTypeCode());

        var fteKh = TransformUtil.isSchoolFundingGroup1orGroup2(groupKh) ? indyFundingResult.getKindHFTE() : "0";
        var fteKf = TransformUtil.isSchoolFundingGroup1orGroup2(groupKf) ? indyFundingResult.getKindFFTE() : "0";
        var fte01 = TransformUtil.isSchoolFundingGroup1orGroup2(group01) ? indyFundingResult.getGrade1FTE() : "0";
        var fte02 = TransformUtil.isSchoolFundingGroup1orGroup2(group02) ? indyFundingResult.getGrade2FTE() : "0";
        var fte03 = TransformUtil.isSchoolFundingGroup1orGroup2(group03) ? indyFundingResult.getGrade3FTE() : "0";
        var fte04 = TransformUtil.isSchoolFundingGroup1orGroup2(group04) ? indyFundingResult.getGrade4FTE() : "0";
        var fte05 = TransformUtil.isSchoolFundingGroup1orGroup2(group05) ? indyFundingResult.getGrade5FTE() : "0";
        var fte06 = TransformUtil.isSchoolFundingGroup1orGroup2(group06) ? indyFundingResult.getGrade6FTE() : "0";
        var fte07 = TransformUtil.isSchoolFundingGroup1orGroup2(group07) ? indyFundingResult.getGrade7FTE() : "0";
        var fteEU = TransformUtil.isSchoolFundingGroup1orGroup2(groupEU) ? indyFundingResult.getGradeEUFTE() : "0";
        var fte08 = TransformUtil.isSchoolFundingGroup1orGroup2(group08) ? indyFundingResult.getGrade8FTE() : "0";
        var fte09 = TransformUtil.isSchoolFundingGroup1orGroup2(group09) ? indyFundingResult.getGrade9FTE() : "0";
        var fte10 = TransformUtil.isSchoolFundingGroup1orGroup2(group10) ? indyFundingResult.getGrade10FTE() : "0";
        var fte11 = TransformUtil.isSchoolFundingGroup1orGroup2(group11) ? indyFundingResult.getGrade11FTE() : "0";
        var fte12 = TransformUtil.isSchoolFundingGroup1orGroup2(group12) ? indyFundingResult.getGrade12FTE() : "0";
        var fteSU = TransformUtil.isSchoolFundingGroup1orGroup2(groupSU) ? indyFundingResult.getGradeSUFTE() : "0";
        var fteGA = TransformUtil.isSchoolFundingGroup1orGroup2(groupGA) ? indyFundingResult.getGradeGAFTE() : "0";
        var fteHS = TransformUtil.isSchoolFundingGroup1orGroup2(groupHS) ? indyFundingResult.getGradeHSFTE() : "0";

        var totalFTE = Double.parseDouble(fteKh) +  Double.parseDouble(fteKf) + Double.parseDouble(fte01) + Double.parseDouble(fte02)
                + Double.parseDouble(fte03) + Double.parseDouble(fte04) + Double.parseDouble(fte05) + Double.parseDouble(fte06) +
                Double.parseDouble(fte07) + Double.parseDouble(fteEU) + Double.parseDouble(fte08) + Double.parseDouble(fte09) +
                Double.parseDouble(fte10) + Double.parseDouble(fte11) + Double.parseDouble(fte12) + Double.parseDouble(fteSU)
                + Double.parseDouble(fteGA) + Double.parseDouble(fteHS);

                csvRowData.addAll(Arrays.asList(
                district != null ? district.getDistrictNumber() : null,
                district != null ? district.getDisplayName() : null,
                authority != null ? authority.getAuthorityNumber() : null,
                authority != null ? authority.getDisplayName() : null,
                school.getSchoolNumber(),
                school.getDisplayName(),
                facilityType.isPresent() ? facilityType.get().getLabel() : school.getFacilityTypeCode(),

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
                Double.toString(totalFTE),

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

                fteKh,
                fteKf,
                fte01,
                fte02,
                fte03,
                fte04,
                fte05,
                fte06,
                fte07,
                fteEU,
                fte08,
                fte09,
                fte10,
                fte11,
                fte12,
                fteSU,
                fteGA,
                fteHS
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
