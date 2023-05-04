package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentValidationIssueEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentValidationIssue;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class})
@SuppressWarnings("squid:S1214")
public interface SdcSchoolCollectionStudentValidationIssueMapper {

    SdcSchoolCollectionStudentValidationIssueMapper mapper = Mappers.getMapper(SdcSchoolCollectionStudentValidationIssueMapper.class);

  SdcSchoolCollectionStudentValidationIssueEntity toModel(SdcSchoolCollectionStudentValidationIssue structure);
  @Mapping(target = "sdcSchoolCollectionStudentID", source = "entity.sdcSchoolCollectionStudentEntity.sdcSchoolCollectionStudentID")
  SdcSchoolCollectionStudentValidationIssue toStructure(SdcSchoolCollectionStudentValidationIssueEntity entity);

}
