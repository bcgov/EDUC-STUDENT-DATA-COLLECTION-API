package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolBatch;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = SdcSchoolStudentMapper.class)
@DecoratedWith(SdcSchoolBatchDecorator.class)
public interface SdcSchoolBatchMapper {

  SdcSchoolBatchMapper mapper = Mappers.getMapper(SdcSchoolBatchMapper.class);

  @Mapping(target = "collectionID", source = "sdcSchoolBatchEntity.sdcEntity.collectionID")
  SdcSchoolBatch toSdcSchoolBatch(SdcSchoolBatchEntity sdcSchoolBatchEntity);

  SdcSchoolBatchEntity toSdcSchoolBatchEntity(SdcSchoolBatch sdcSchoolBatch);

}
