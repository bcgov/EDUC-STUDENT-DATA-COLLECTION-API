package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolBatch;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Slf4j
public abstract class SdcSchoolBatchDecorator implements SdcSchoolBatchMapper {

  private final SdcSchoolBatchMapper delegate;

  protected SdcSchoolBatchDecorator(SdcSchoolBatchMapper delegate) {
    this.delegate = delegate;
  }

  @Override
  public SdcSchoolBatch toSdcSchoolBatch(SdcSchoolBatchEntity sdcSchoolBatchEntity) {
    final var batch = this.delegate.toSdcSchoolBatch(sdcSchoolBatchEntity);
    SdcSchoolStudentMapper studentMapper = SdcSchoolStudentMapper.mapper;
    sdcSchoolBatchEntity.getSDCSchoolStudentEntities().stream().forEach(student -> {
      batch.setStudents(new ArrayList<>());
      batch.getStudents().add(studentMapper.toSdcSchoolStudent(student));
    });
    return batch;
  }



}
