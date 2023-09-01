package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;


import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.mappers.UUIDMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.penmatch.v1.PenMatchStudent;
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

  @Mapping(target = "usualMiddleName", source = "sdcSchoolStudent.usualMiddleNames")
  @Mapping(target = "usualGivenName", source = "sdcSchoolStudent.usualFirstName")
  @Mapping(target = "usualSurname", source = "sdcSchoolStudent.usualLastName")
  @Mapping(target = "mincode", source = "mincode")
  @Mapping(target = "pen", source = "sdcSchoolStudent.studentPen")
  @Mapping(target = "middleName", source = "sdcSchoolStudent.legalMiddleNames")
  @Mapping(target = "sex", source = "sdcSchoolStudent.gender")
  @Mapping(target = "givenName", source = "sdcSchoolStudent.legalFirstName")
  @Mapping(target = "dob", source = "sdcSchoolStudent.dob")
  @Mapping(target = "postal", source = "sdcSchoolStudent.postalCode")
  @Mapping(target = "surname", source = "sdcSchoolStudent.legalLastName")
  @Mapping(target = "localID", source = "sdcSchoolStudent.localID")
  @Mapping(target = "enrolledGradeCode", source = "sdcSchoolStudent.enrolledGradeCode")
  PenMatchStudent toPenMatchStudent(SdcSchoolCollectionStudentEntity sdcSchoolStudent, String mincode);
}
