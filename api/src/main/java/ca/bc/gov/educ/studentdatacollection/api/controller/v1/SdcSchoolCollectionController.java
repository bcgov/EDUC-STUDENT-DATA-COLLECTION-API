package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionMapper;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Slf4j
public class SdcSchoolCollectionController implements SdcSchoolCollectionEndpoint {

    private static final SdcSchoolCollectionMapper mapper = SdcSchoolCollectionMapper.mapper;

    private final SdcSchoolCollectionService sdcSchoolCollectionService;

    public SdcSchoolCollectionController(SdcSchoolCollectionService sdcSchoolCollectionService) {
        this.sdcSchoolCollectionService = sdcSchoolCollectionService;
    }

    @Override
    public SdcSchoolCollection getSchoolCollection(UUID sdcSchoolCollectionID) {
        return mapper.toSdcSchoolBatch(sdcSchoolCollectionService.getSdcSchoolCollection(sdcSchoolCollectionID));
    }

    @Override
    public SdcSchoolCollection getSchoolCollectionBySchoolId(UUID schoolID) {
        return mapper.toSdcSchoolBatch(sdcSchoolCollectionService.getSdcSchoolCollectionBySchoolID(schoolID));
    }
}
