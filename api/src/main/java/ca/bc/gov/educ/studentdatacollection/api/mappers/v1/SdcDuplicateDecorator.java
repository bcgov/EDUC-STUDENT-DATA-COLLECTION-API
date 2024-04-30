package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDuplicateEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcDuplicate;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.factory.Mappers;

@Slf4j
public abstract class SdcDuplicateDecorator implements SdcDuplicateMapper {

  private final SdcDuplicateMapper delegate;
  SdcSchoolCollectionStudentMapper studentMapper = Mappers.getMapper(SdcSchoolCollectionStudentMapper.class);

  protected SdcDuplicateDecorator(SdcDuplicateMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcDuplicate toSdcDuplicate(SdcDuplicateEntity sdcDuplicateEntity) {
    final var duplicate = this.delegate.toSdcDuplicate(sdcDuplicateEntity);
    var student1 = sdcDuplicateEntity.getSdcDuplicateStudentEntities().iterator().next().getSdcSchoolCollectionStudentEntity();
    var student2 = sdcDuplicateEntity.getSdcDuplicateStudentEntities().iterator().next().getSdcSchoolCollectionStudentEntity();

    duplicate.setSdcSchoolCollectionStudent1Entity(studentMapper.toSdcSchoolStudent(student1));
    duplicate.setSdcSchoolCollectionStudent2Entity(studentMapper.toSdcSchoolStudent(student2));
    duplicate.setRetainedSdcSchoolCollectionStudentEntity(studentMapper.toSdcSchoolStudent(sdcDuplicateEntity.getRetainedSdcSchoolCollectionStudentEntity()));

    return duplicate;
  }

}
