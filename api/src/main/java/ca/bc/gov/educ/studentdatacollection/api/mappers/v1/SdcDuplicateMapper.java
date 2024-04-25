package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
public interface SdcDuplicateMapper {

  SdcDuplicateMapper mapper = Mappers.getMapper(SdcDuplicateMapper.class);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  SdcDuplicate toSdcDuplicate(SdcDuplicateEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollection.sdcSchoolCollectionID", source = "sdcSchoolCollectionID")
  SdcDuplicateEntity toSdcDuplicateEntity(SdcDuplicate sdcSchoolStudent);

}
