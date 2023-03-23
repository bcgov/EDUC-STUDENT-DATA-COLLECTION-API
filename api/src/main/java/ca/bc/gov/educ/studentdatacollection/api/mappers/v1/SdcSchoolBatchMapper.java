package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = SdcSchoolCollectionStudentMapper.class)
@DecoratedWith(SdcSchoolBatchDecorator.class)
public interface SdcSchoolBatchMapper {

  SdcSchoolBatchMapper mapper = Mappers.getMapper(SdcSchoolBatchMapper.class);

  @Mapping(target = "collectionID", source = "sdcSchoolCollectionEntity.collectionEntity.collectionID")
  @Mapping(target = "students", ignore = true)
  SdcSchoolCollection toSdcSchoolBatch(SdcSchoolCollectionEntity sdcSchoolCollectionEntity);

  SdcSchoolCollectionEntity toSdcSchoolBatchEntity(SdcSchoolCollection sdcSchoolBatch);

}
