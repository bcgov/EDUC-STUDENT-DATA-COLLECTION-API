package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;

@Slf4j
public abstract class SdcSchoolCollectionStudentDecorator implements SdcSchoolCollectionStudentMapper {

  private final SdcSchoolCollectionStudentMapper delegate;

  protected SdcSchoolCollectionStudentDecorator(SdcSchoolCollectionStudentMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity) {
    final SdcSchoolCollectionStudent sdcSchoolCollectionStudent = this.delegate.toSdcSchoolStudent(sdcSchoolCollectionStudentEntity);
    SdcSchoolCollectionStudentValidationIssueMapper studentValidationIssueMapper = SdcSchoolCollectionStudentValidationIssueMapper.mapper;
    SdcSchoolCollectionStudentEnrolledProgramMapper sdcSchoolCollectionStudentEnrolledProgramMapper = SdcSchoolCollectionStudentEnrolledProgramMapper.mapper;
    sdcSchoolCollectionStudent.setSdcSchoolCollectionStudentEnrolledPrograms(new ArrayList<>());
    sdcSchoolCollectionStudent.setSdcSchoolCollectionStudentValidationIssues(new ArrayList<>());
    sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().stream().forEach(program -> sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentEnrolledPrograms().add(sdcSchoolCollectionStudentEnrolledProgramMapper.toStructure(program)));
    sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().stream().forEach(issue -> sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentValidationIssues().add(studentValidationIssueMapper.toStructure(issue)));
    return sdcSchoolCollectionStudent;
  }

  @Override
  public SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(SdcSchoolCollectionStudentPaginationEntity sdcSchoolCollectionStudentEntity) {
    final SdcSchoolCollectionStudent sdcSchoolCollectionStudent = this.delegate.toSdcSchoolStudent(sdcSchoolCollectionStudentEntity);
    SdcSchoolCollectionStudentValidationIssueMapper studentValidationIssueMapper = SdcSchoolCollectionStudentValidationIssueMapper.mapper;
    SdcSchoolCollectionStudentEnrolledProgramMapper sdcSchoolCollectionStudentEnrolledProgramMapper = SdcSchoolCollectionStudentEnrolledProgramMapper.mapper;
    SdcStudentEllMapper sdcStudentEllMapper = SdcStudentEllMapper.mapper;
    sdcSchoolCollectionStudent.setSdcStudentEll(sdcStudentEllMapper.toStructure(sdcSchoolCollectionStudentEntity.getSdcStudentEllEntity()));
    sdcSchoolCollectionStudent.setSdcSchoolCollectionStudentEnrolledPrograms(new ArrayList<>());
    sdcSchoolCollectionStudent.setSdcSchoolCollectionStudentValidationIssues(new ArrayList<>());
    sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().stream().forEach(program -> sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentEnrolledPrograms().add(sdcSchoolCollectionStudentEnrolledProgramMapper.toStructure(program)));
    sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().stream().forEach(issue -> sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentValidationIssues().add(studentValidationIssueMapper.toStructure(issue)));
    return sdcSchoolCollectionStudent;
  }
}
