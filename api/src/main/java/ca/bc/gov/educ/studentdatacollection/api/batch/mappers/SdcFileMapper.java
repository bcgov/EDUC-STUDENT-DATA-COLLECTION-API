package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;


import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.factory.Mappers;

/**
 * The interface Pen request batch file mapper.
 */
@Mapper
@DecoratedWith(SdcBatchFileDecorator.class)
public interface SdcFileMapper {
  /**
   * The constant mapper.
   */
  SdcFileMapper mapper = Mappers.getMapper(SdcFileMapper.class);
  /**
   * The constant PEN_REQUEST_BATCH_API.
   */
  String PEN_REQUEST_BATCH_API = "PEN_REQUEST_BATCH_API";

  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  @Mapping(target = "ministryPRBSourceCode", ignore = true)
  @Mapping(target = "extractDate", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "schoolGroupCode", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "newPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "penRequestBatchProcessTypeCode", expression = "java( ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode())")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getProductID() ))", target = "sisProductID")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getProductName() ))", target = "sisProductName")
  @Mapping(expression = "java( org.apache.commons.lang3.StringUtils.trim(file.getBatchFileTrailer().getVendorName() ))", target = "sisVendorName", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
  SdcSchoolBatchEntity toSdcBatchEntityLoaded(SdcBatchFile file);

  @Mapping(target = "repeatRequestSequenceNumber", ignore = true)
  @Mapping(target = "repeatRequestOriginalID", ignore = true)
  @Mapping(target = "recordNumber", ignore = true)
  @Mapping(target = "questionableMatchStudentId", ignore = true)
  @Mapping(target = "penRequestBatchStudentValidationIssueEntities", ignore = true)
  @Mapping(target = "matchAlgorithmStatusCode", ignore = true)
  @Mapping(target = "infoRequest", ignore = true)
  @Mapping(target = "bestMatchPEN", ignore = true)
  @Mapping(target = "studentID", ignore = true)
  @Mapping(target = "penRequestBatchStudentStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentID", ignore = true)
  @Mapping(target = "penRequestBatchEntity", ignore = true)
  @Mapping(target = "assignedPEN", ignore = true)
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolStudentEntity toSdcSchoolStudentEntity(StudentDetails studentDetails, SdcSchoolBatchEntity sdcSchoolBatchEntity);

  @Mapping(target = "penRequestBatchHistoryEntities", ignore = true)
  @Mapping(target = "studentCount", ignore = true)
  @Mapping(target = "mincode", ignore = true)
  @Mapping(target = "sisVendorName", ignore = true)
  @Mapping(target = "sisProductName", ignore = true)
  @Mapping(target = "sisProductID", ignore = true)
  @Mapping(target = "schoolName", ignore = true)
  @Mapping(target = "schoolGroupCode", ignore = true)
  @Mapping(target = "repeatCount", ignore = true)
  @Mapping(target = "processDate", ignore = true)
  @Mapping(target = "penRequestBatchTypeCode", ignore = true)
  @Mapping(target = "penRequestBatchStudentEntities", ignore = true)
  @Mapping(target = "penRequestBatchStatusReason", ignore = true)
  @Mapping(target = "penRequestBatchStatusCode", ignore = true)
  @Mapping(target = "penRequestBatchID", ignore = true)
  @Mapping(target = "officeNumber", ignore = true)
  @Mapping(target = "ministryPRBSourceCode", ignore = true)
  @Mapping(target = "matchedCount", ignore = true)
  @Mapping(target = "newPenCount", ignore = true)
  @Mapping(target = "fixableCount", ignore = true)
  @Mapping(target = "extractDate", ignore = true)
  @Mapping(target = "errorCount", ignore = true)
  @Mapping(target = "email", ignore = true)
  @Mapping(target = "contactName", ignore = true)
  @Mapping(source = "penWebBlobEntity.insertDateTime", target = "insertDate")
  @Mapping(source = "penWebBlobEntity.studentCount", target = "sourceStudentCount")
  @Mapping(target = "penRequestBatchProcessTypeCode", expression = "java( ca.bc.gov.educ.penreg.api.constants.PenRequestBatchProcessTypeCodes.FLAT_FILE.getCode())")
  @Mapping(target = "updateUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
  @Mapping(target = "createUser", constant = PEN_REQUEST_BATCH_API)
  @Mapping(target = "createDate", expression = "java(java.time.LocalDateTime.now() )")
  SdcSchoolBatchEntity toPenReqBatchEntityForBusinessException(String reason, SdcBatchStatusCodes penRequestBatchStatusCode, SdcBatchFile batchFile, boolean persistStudentRecords);

}
