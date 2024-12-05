package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.HeadcountReportTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcDistrictCollectionHeadcountReports;
import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcDistrictCollectionHeadcountService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SdcDistrictCollectionHeadcountReportsController implements SdcDistrictCollectionHeadcountReports {

    private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;
    private final SdcDistrictCollectionHeadcountService sdcDistrictCollectionHeadcountService;
    @Override
    public SdcSchoolCollectionStudentHeadcounts getSdcSchoolCollectionStudentHeadcounts(UUID sdcDistrictCollectionID, String type, boolean compare) {
        var sdcDistrictCollectionEntity = sdcDistrictCollectionRepository.findById(sdcDistrictCollectionID).orElseThrow(() ->
                new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));
        if (HeadcountReportTypeCodes.ENROLLMENT.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getEnrollmentHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.SPECIAL_ED.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getSpecialEdHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.GRADE_ENROLLMENT.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getGradeEnrollmentHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.SPECIAL_ED_PER_SCHOOL.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getSpecialEdHeadcountsPerSchool(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.SPECIAL_ED_CAT_PER_SCHOOL.getCode().equals(type)){
            return sdcDistrictCollectionHeadcountService.getSpecialEdCatHeadcountsPerSchool(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.CAREER .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getCareerHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.FRENCH .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getFrenchHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.CAREER_PER_SCHOOL.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getCareerPerSchoolHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.FRENCH_PER_SCHOOL.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getFrenchHeadcountsPerSchool(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.INDIGENOUS.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getIndigenousHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.INDIGENOUS_PER_SCHOOL.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getIndigenousHeadcountsPerSchool(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.BAND_CODES.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getBandResidenceHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.BAND_CODES_PER_SCHOOL.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getBandResidenceHeadcountsPerSchool(sdcDistrictCollectionEntity, compare);
        }else if (HeadcountReportTypeCodes.ELL .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getEllHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.ELL_PER_SCHOOL .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getEllPerSchoolHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.REFUGEE_PER_SCHOOL .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getRefugeePerSchoolHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.ZERO_FTE_SUMMARY.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getZeroFTESummaryHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.INCLUSIVE_EDUCATION_VARIANCE .getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getSpecialEdVarianceHeadcounts(sdcDistrictCollectionEntity);
        }
        return null;
    }
}
