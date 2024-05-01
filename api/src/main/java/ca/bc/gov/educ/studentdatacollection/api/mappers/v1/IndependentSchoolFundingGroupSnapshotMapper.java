package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupSnapshotEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroupSnapshot;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface IndependentSchoolFundingGroupSnapshotMapper {

  IndependentSchoolFundingGroupSnapshotMapper mapper = Mappers.getMapper(IndependentSchoolFundingGroupSnapshotMapper.class);

  IndependentSchoolFundingGroupSnapshotEntity toModel(IndependentSchoolFundingGroupSnapshot structure);

  IndependentSchoolFundingGroupSnapshot toStructure(IndependentSchoolFundingGroupSnapshotEntity entity);
}
