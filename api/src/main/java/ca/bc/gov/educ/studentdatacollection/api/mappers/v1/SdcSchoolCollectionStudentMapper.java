package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@DecoratedWith(SdcSchoolCollectionStudentDecorator.class)
public interface SdcSchoolCollectionStudentMapper {

  SdcSchoolCollectionStudentMapper mapper = Mappers.getMapper(SdcSchoolCollectionStudentMapper.class);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(SdcSchoolCollectionStudent sdcSchoolStudent);

}
