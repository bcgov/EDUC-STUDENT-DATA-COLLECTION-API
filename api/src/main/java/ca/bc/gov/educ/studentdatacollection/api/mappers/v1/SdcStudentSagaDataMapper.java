package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.SdcStudentSagaData;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch student saga data mapper.
 */
@Mapper(uses = UUIDMapper.class)
public interface SdcStudentSagaDataMapper {
  /**
   * The constant mapper.
   */
  SdcStudentSagaDataMapper mapper = Mappers.getMapper(SdcStudentSagaDataMapper.class);

  /**
   * To pen req batch student saga data pen request batch student saga data.
   *
   * @param entity the entity
   * @return the pen request batch student saga data
   */
  @Mapping(target = "penMatchResult", ignore = true)
  SdcStudentSagaData toSdcStudentSagaData(SdcSchoolStudentEntity entity);
}
