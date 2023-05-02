package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class SdcSchoolCollectionStudentDecorator implements SdcSchoolCollectionStudentMapper {

  private final SdcSchoolCollectionStudentMapper delegate;

  protected SdcSchoolCollectionStudentDecorator(SdcSchoolCollectionStudentMapper delegate) {
    this.delegate = delegate;
  }
  @Override
  public SdcSchoolCollectionStudent toSdcSchoolCollectionStudentWithValidationIssues(
      SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity) {
    final SdcSchoolCollectionStudent sdcSchoolCollectionStudent = this.delegate.toSdcSchoolStudent(sdcSchoolCollectionStudentEntity);
    SdcSchoolCollectionStudentValidationIssueMapper studentValidationIssueMapper = SdcSchoolCollectionStudentValidationIssueMapper.mapper;
    sdcSchoolCollectionStudent.setSdcSchoolCollectionStudentValidationIssues(new ArrayList<>());
    sdcSchoolCollectionStudentEntity.getSDCStudentValidationIssueEntities().stream().forEach(issue -> sdcSchoolCollectionStudent.getSdcSchoolCollectionStudentValidationIssues().add(studentValidationIssueMapper.toStructure(issue)));
    return sdcSchoolCollectionStudent;
  }
}
