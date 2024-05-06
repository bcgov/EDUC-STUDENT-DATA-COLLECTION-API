package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
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
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolStudent(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollectionID", source = "sdcSchoolCollection.sdcSchoolCollectionID")
  @Mapping(target = "sdcDistrictCollectionID", source = "sdcSchoolCollection.sdcDistrictCollectionID")
  @Mapping(target = "schoolID", source = "sdcSchoolCollection.schoolID")
  SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentPaginationEntity sdcSchoolStudentEntity);

  @Mapping(target = "sdcSchoolCollection.sdcSchoolCollectionID", source = "sdcSchoolCollectionID")
  SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(SdcSchoolCollectionStudent sdcSchoolStudent);

}
