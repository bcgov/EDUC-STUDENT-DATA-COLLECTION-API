package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;

import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.StudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.constants.SdcBatchStatusCodes;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolBatchEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * The type Pen request batch file decorator.
 */
@Slf4j
public abstract class SdcBatchFileDecorator implements SdcBatchFileMapper {
  private static final String BOOLEAN_YES = "Y";
  private final SdcBatchFileMapper delegate;

  protected SdcBatchFileDecorator(final SdcBatchFileMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public SdcSchoolBatchEntity toSdcBatchEntityLoaded(final SdcBatchFile file,final SdcFileUpload upload) {
    final var entity = this.delegate.toSdcBatchEntityLoaded(file, upload);
    entity.setStatusCode(SdcBatchStatusCodes.LOADED.getCode());
    entity.setSchoolID(UUID.fromString(upload.getSchoolID()));
    entity.setUploadFileName(upload.getFileName());
    return entity;
  }

  @Override
  public SdcSchoolBatchEntity toSdcSchoolBatchEntityForBusinessException(final String reason, final SdcBatchStatusCodes sdcBatchStatusCodes, final SdcBatchFile batchFile, final boolean persistStudentRecords) {
    final var entity = this.delegate.toSdcSchoolBatchEntityForBusinessException(reason, sdcBatchStatusCodes, batchFile, persistStudentRecords);
    entity.setStatusCode(sdcBatchStatusCodes.getCode());
//    entity.setPenRequestBatchStatusReason(reason);
    if (persistStudentRecords && batchFile != null) { // for certain business exception, system needs to store the student details as well.
      for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
        final var sdcSchoolStudentEntity = this.toSdcSchoolStudentEntity(student, entity);
        entity.getSDCSchoolStudentEntities().add(sdcSchoolStudentEntity);
      }
    }
    return entity;
  }

  @Override
  public SdcSchoolStudentEntity toSdcSchoolStudentEntity(final StudentDetails studentDetails, final SdcSchoolBatchEntity sdcSchoolBatchEntity) {
    final var entity = this.delegate.toSdcSchoolStudentEntity(studentDetails, sdcSchoolBatchEntity);
    entity.setSdcSchoolBatchEntity(sdcSchoolBatchEntity); // add thePK/FK relationship
    entity.setStatusCode(SdcBatchStatusCodes.LOADED.getCode());

    entity.setPostalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getPostalCode()));
    entity.setGenderCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getGender()));
    entity.setDob(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getBirthDate()));
    entity.setEnrolledGradeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getEnrolledGradeCode()));
    entity.setLegalLastName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getLegalSurname()));
    entity.setLegalFirstName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getLegalGivenName()));
    entity.setLegalMiddleNames(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getLegalMiddleName()));
    entity.setUsualLastName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getUsualSurname()));
    entity.setUsualFirstName(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getUsualGivenName()));
    entity.setUsualMiddleNames(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getUsualMiddleName()));
    entity.setLocalID(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getLocalStudentID()));
    entity.setSpecialEducationCategoryCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getSpecialEducationCategory()));
    entity.setSchoolFundingCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getSchoolFundingCode()));
    entity.setNativeIndianAncestryInd(getBooleanFromYNField(studentDetails.getNativeAncestryIndicator()));
    entity.setHomeLanguageSpokenTypeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getHomeSpokenLanguageCode()));
    entity.setOtherCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getOtherCourses()));
    entity.setSupportBlocks(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getSupportBlocks()));
    entity.setEnrolledProgramCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getEnrolledProgramCodes()));
    entity.setCareerProgramCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getCareerProgramCode()));
    entity.setNumberOfCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getNumberOfCourses()));
    entity.setBandTypeCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getBandCode()));

    if(StringUtils.isNotBlank(studentDetails.getPen()) && studentDetails.getPen().length() == 9) {
      entity.setStudentPen(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getPen()));
    }

    return entity;
  }

  private boolean getBooleanFromYNField(String value){
    return !StringUtils.isEmpty(value) && value.equals(BOOLEAN_YES);
  }

}
