package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface SdcSchoolStudentMapper {

  SdcSchoolStudentMapper mapper = Mappers.getMapper(SdcSchoolStudentMapper.class);

  @Mapping(target = "sdcSchoolBatchID", source = "sdcSchoolBatchEntity.sdcSchoolBatchID")
  SdcSchoolStudent toSdcSchoolStudent(SdcSchoolStudentEntity sdcSchoolStudentEntity);

  SdcSchoolStudentEntity toSdcSchoolStudentEntity(SdcSchoolStudent sdcSchoolStudent);

}
