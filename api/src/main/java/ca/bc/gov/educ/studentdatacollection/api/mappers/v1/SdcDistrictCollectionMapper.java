package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDistrictCollection;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface SdcDistrictCollectionMapper {

  SdcDistrictCollectionMapper mapper = Mappers.getMapper(SdcDistrictCollectionMapper.class);

  SdcDistrictCollectionEntity toModel(SdcDistrictCollection sdcDistrictCollection);

  @Mapping(target = "collectionID", source = "sdcDistrictCollectionEntity.collectionEntity.collectionID")
  @Mapping(target = "collectionTypeCode", source = "sdcDistrictCollectionEntity.collectionEntity.collectionTypeCode")
  SdcDistrictCollection toStructure(SdcDistrictCollectionEntity sdcDistrictCollectionEntity);
}
