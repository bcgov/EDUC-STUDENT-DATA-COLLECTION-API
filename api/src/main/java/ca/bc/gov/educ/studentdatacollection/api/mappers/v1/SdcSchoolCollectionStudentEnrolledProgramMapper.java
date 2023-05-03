package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentEnrolledProgram;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class})
@SuppressWarnings("squid:S1214")
public interface SdcSchoolCollectionStudentEnrolledProgramMapper {

  SdcSchoolCollectionStudentEnrolledProgramMapper mapper = Mappers.getMapper(SdcSchoolCollectionStudentEnrolledProgramMapper.class);

  SdcSchoolCollectionStudentEnrolledProgramEntity toModel(SdcSchoolCollectionStudentEnrolledProgram structure);
  @Mapping(target = "sdcSchoolCollectionStudentID", source = "sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID")
  SdcSchoolCollectionStudentEnrolledProgram toStructure(SdcSchoolCollectionStudentEnrolledProgramEntity entity);

}
