package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollection;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public abstract class SdcSchoolBatchDecorator implements SdcSchoolBatchMapper {

  private final SdcSchoolBatchMapper delegate;

  protected SdcSchoolBatchDecorator(SdcSchoolBatchMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcSchoolCollection toSdcSchoolBatch(SdcSchoolCollectionEntity sdcSchoolCollectionEntity) {
    final var batch = this.delegate.toSdcSchoolBatch(sdcSchoolCollectionEntity);
    SdcSchoolCollectionStudentMapper studentMapper = SdcSchoolCollectionStudentMapper.mapper;
    sdcSchoolCollectionEntity.getSDCSchoolStudentEntities().stream().forEach(student -> {
      batch.setStudents(new ArrayList<>());
      batch.getStudents().add(studentMapper.toSdcSchoolStudent(student));
    });
    return batch;
  }



}
