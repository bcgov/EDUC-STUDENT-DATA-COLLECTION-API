package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;


import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcStudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
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

  @Mapping(target = "sdcSchoolCollectionStatusCode", ignore = true)
  @Mapping(target = "sdcSchoolCollectionID", ignore = true)
  @Mapping(target = "uploadDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "updateUser", source = "upload.updateUser")
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolCollectionEntity toSdcBatchEntityLoaded(final SdcBatchFile file, final SdcFileUpload upload, final String sdcSchoolCollectionID);

  @Mapping(target = "sdcSchoolCollectionStudentStatusCode", ignore = true)
  @Mapping(target = "sdcSchoolCollectionStudentID", ignore = true)
  @Mapping(target = "studentPen", ignore = true)
  @Mapping(target = "numberOfCoursesDec", ignore = true)
  @Mapping(target = "updateUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(SdcStudentDetails studentDetails, SdcSchoolCollectionEntity sdcSchoolBatchEntity);

  @Mapping(target = "sdcSchoolCollectionStatusCode", ignore = true)
  @Mapping(target = "sdcSchoolCollectionID", ignore = true)
  @Mapping(target = "updateUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = STUDENT_DATA_COLLECTION_API)
  @Mapping(target = "createDate", expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolCollectionEntity toSdcSchoolBatchEntityForBusinessException(String reason, SdcSchoolCollectionStatus penRequestBatchStatusCode, SdcBatchFile batchFile, boolean persistStudentRecords);

}
