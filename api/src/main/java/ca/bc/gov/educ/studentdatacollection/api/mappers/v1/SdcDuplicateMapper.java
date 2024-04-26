package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = UUIDMapper.class)
@DecoratedWith(SdcDuplicateDecorator.class)
public interface SdcDuplicateMapper {

  SdcDuplicateMapper mapper = Mappers.getMapper(SdcDuplicateMapper.class);

  SdcDuplicate toSdcDuplicate(SdcDuplicateEntity sdcSchoolStudentEntity);

}
