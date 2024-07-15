package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionSubmissionSignatureEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollectionSubmissionSignature;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface SdcDistrictCollectionSubmissionSignatureMapper {

    SdcDistrictCollectionSubmissionSignatureMapper mapper = Mappers.getMapper(SdcDistrictCollectionSubmissionSignatureMapper.class);

    SdcDistrictCollectionSubmissionSignatureEntity toModel(SdcDistrictCollectionSubmissionSignature sdcDistrictCollection);

    @Mapping(target = "sdcDistrictCollectionID", source = "sdcDistrictCollectionSubmissionSignatureEntity.sdcDistrictCollection.sdcDistrictCollectionID")
    SdcDistrictCollectionSubmissionSignature toStructure(SdcDistrictCollectionSubmissionSignatureEntity sdcDistrictCollectionSubmissionSignatureEntity);

}
