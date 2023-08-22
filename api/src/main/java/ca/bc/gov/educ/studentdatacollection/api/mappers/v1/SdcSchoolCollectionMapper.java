package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = SdcSchoolCollectionStudentMapper.class)
@DecoratedWith(SdcSchoolBatchDecorator.class)
public interface SdcSchoolCollectionMapper {

  SdcSchoolCollectionMapper mapper = Mappers.getMapper(SdcSchoolCollectionMapper.class);

  @Mapping(target = "collectionID", source = "sdcSchoolCollectionEntity.collectionEntity.collectionID")
  @Mapping(target = "collectionTypeCode", source = "sdcSchoolCollectionEntity.collectionEntity.collectionTypeCode")
  @Mapping(target = "collectionOpenDate", source = "sdcSchoolCollectionEntity.collectionEntity.openDate")
  @Mapping(target = "collectionCloseDate", source = "sdcSchoolCollectionEntity.collectionEntity.closeDate")
  @Mapping(target = "students", ignore = true)
  SdcSchoolCollection toSdcSchoolBatch(SdcSchoolCollectionEntity sdcSchoolCollectionEntity);

  SdcSchoolCollectionEntity toSdcSchoolBatchEntity(SdcSchoolCollection sdcSchoolBatch);

  @Mapping(target = "uploadDate", expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolCollectionEntity toModel(SdcSchoolCollection sdcSchoolCollection);

}
