package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;


import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcStudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch file mapper.
 */
@Mapper
@DecoratedWith(SdcBatchFileDecorator.class)
public interface SdcBatchFileMapper {
  /**
   * The constant mapper.
   */
  SdcBatchFileMapper mapper = Mappers.getMapper(SdcBatchFileMapper.class);
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  String STUDENT_DATA_COLLECTION_API = "STUDENT_DATA_COLLECTION_API";

  @Mapping(target = "statusCode", ignore = true)
  @Mapping(target = "sdcSchoolBatchID", ignore = true)
  @Mapping(target = "uploadDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "updateUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolBatchEntity toSdcBatchEntityLoaded(SdcBatchFile file, SdcFileUpload upload);

  @Mapping(target = "statusCode", ignore = true)
  @Mapping(target = "sdcSchoolStudentID", ignore = true)
  @Mapping(target = "sdcSchoolBatchEntity", ignore = true)
  @Mapping(target = "studentPen", ignore = true)
  @Mapping(target = "updateUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolStudentEntity toSdcSchoolStudentEntity(SdcStudentDetails studentDetails, SdcSchoolBatchEntity sdcSchoolBatchEntity);

  @Mapping(target = "statusCode", ignore = true)
  @Mapping(target = "sdcSchoolBatchID", ignore = true)
  @Mapping(target = "updateUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate", expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolBatchEntity toSdcSchoolBatchEntityForBusinessException(String reason, SdcBatchStatusCodes penRequestBatchStatusCode, SdcBatchFile batchFile, boolean persistStudentRecords);

}
