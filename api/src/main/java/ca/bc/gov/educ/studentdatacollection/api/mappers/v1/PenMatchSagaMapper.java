package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen match saga mapper.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@SuppressWarnings("squid:S1214")
public interface PenMatchSagaMapper {
  /**
   * The constant mapper.
   */
  PenMatchSagaMapper mapper = Mappers.getMapper(PenMatchSagaMapper.class);

  @Mapping(target = "usualSurname", ignore = true)
  @Mapping(target = "usualMiddleName", ignore = true)
  @Mapping(target = "usualGivenName", ignore = true)
  @Mapping(target = "postal", ignore = true)
  @Mapping(target = "pen", ignore = true)
  @Mapping(target = "middleName", ignore = true)
  @Mapping(target = "localID", ignore = true)
  @Mapping(target = "sex", source = "sdcSchoolStudent.gender")
  @Mapping(target = "givenName", source = "sdcSchoolStudent.legalFirstName")
  @Mapping(target = "enrolledGradeCode", ignore = true)
  @Mapping(target = "dob", source = "sdcSchoolStudent.dob")
  @Mapping(target = "surname", source = "sdcSchoolStudent.legalLastName")
  PenMatchStudent toPenMatchStudent(SdcSchoolStudent sdcSchoolStudent, String mincode);
}
