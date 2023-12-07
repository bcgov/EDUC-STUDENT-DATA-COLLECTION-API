package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcStudentEll;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class})
@SuppressWarnings("squid:S1214")
public interface SdcStudentEllMapper {

  SdcStudentEllMapper mapper = Mappers.getMapper(SdcStudentEllMapper.class);

  SdcStudentEllEntity toModel(SdcStudentEll structure);

  SdcStudentEll toStructure(SdcStudentEllEntity entity);

}
