package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcInDistrictDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcInDistrictDuplicate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
public interface SdcDuplicateMapper {

  SdcDuplicateMapper mapper = Mappers.getMapper(SdcDuplicateMapper.class);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  SdcInDistrictDuplicate toSdcDuplicate(SdcInDistrictDuplicateEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollection.sdcSchoolCollectionID", source = "sdcSchoolCollectionID")
  SdcInDistrictDuplicateEntity toSdcDuplicateEntity(SdcInDistrictDuplicate sdcSchoolStudent);

}
