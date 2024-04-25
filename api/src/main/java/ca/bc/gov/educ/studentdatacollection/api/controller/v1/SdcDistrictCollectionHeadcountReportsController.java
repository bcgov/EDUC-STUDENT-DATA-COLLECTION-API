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
        var sdcDistrictCollectionEntity = sdcDistrictCollectionRepository.findBySdcDistrictCollectionID(sdcDistrictCollectionID).orElseThrow(() ->
                new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcDistrictCollectionID", sdcDistrictCollectionID.toString()));
        if (HeadcountReportTypeCodes.ENROLLMENT.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getEnrollmentHeadcounts(sdcDistrictCollectionEntity, compare);
        } else if (HeadcountReportTypeCodes.SPECIAL_ED.getCode().equals(type)) {
            return sdcDistrictCollectionHeadcountService.getSpecialEdHeadcounts(sdcDistrictCollectionEntity, compare);
        }
        return null;
    }
}
