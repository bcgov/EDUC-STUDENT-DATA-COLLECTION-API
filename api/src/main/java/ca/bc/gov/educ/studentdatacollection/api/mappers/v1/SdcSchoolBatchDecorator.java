package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public abstract class SdcSchoolBatchDecorator implements SdcSchoolCollectionMapper {

  private final SdcSchoolCollectionMapper delegate;

  protected SdcSchoolBatchDecorator(SdcSchoolCollectionMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcSchoolCollection toSdcSchoolWithStudents(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    final var batch = this.delegate.toSdcSchoolWithStudents(sdcSchoolCollectionEntity);
    SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
    batch.setStudents(new ArrayList<>());
    sdcSchoolCollectionEntity.getSDCSchoolStudentEntities().stream().forEach(student -> batch.getStudents().add(studentMapper.toSdcSchoolStudent(student)));
    return batch;
  }



}
