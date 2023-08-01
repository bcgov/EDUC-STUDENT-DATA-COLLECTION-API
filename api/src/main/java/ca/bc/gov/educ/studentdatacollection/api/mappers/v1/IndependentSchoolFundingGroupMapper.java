package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroup;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface IndependentSchoolFundingGroupMapper {

  IndependentSchoolFundingGroupMapper mapper = Mappers.getMapper(IndependentSchoolFundingGroupMapper.class);

  IndependentSchoolFundingGroupEntity toModel(IndependentSchoolFundingGroup structure);

  IndependentSchoolFundingGroup toStructure(IndependentSchoolFundingGroupEntity entity);

}
