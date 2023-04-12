package ca.bc.gov.educ.studentdatacollection.api.batch.mappers;

import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcBatchFile;
import ca.bc.gov.educ.studentdatacollection.api.batch.struct.SdcStudentDetails;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SdcSchoolCollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.mappers.StringMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.UUID;

/**
 * The type Pen request batch file decorator.
 */
@Slf4j
public abstract class SdcBatchFileDecorator implements SdcBatchFileMapper {
  private final SdcBatchFileMapper delegate;

  protected SdcBatchFileDecorator(final SdcBatchFileMapper mapper) {
    this.delegate = mapper;
  }

  @Override
  public SdcSchoolCollectionEntity toSdcBatchEntityLoaded(final SdcBatchFile file, final SdcFileUpload upload, final String sdcSchoolCollectionID) {
    final var entity = this.delegate.toSdcBatchEntityLoaded(file, upload, sdcSchoolCollectionID);
    entity.setSdcSchoolCollectionStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());
    entity.setSdcSchoolCollectionID(UUID.fromString(sdcSchoolCollectionID));
    entity.setUploadFileName(upload.getFileName());
    return entity;
  }

  @Override
  public SdcSchoolCollectionEntity toSdcSchoolBatchEntityForBusinessException(final String reason, final SdcSchoolCollectionStatus sdcBatchStatusCodes, final SdcBatchFile batchFile, final boolean persistStudentRecords) {
    final var entity = this.delegate.toSdcSchoolBatchEntityForBusinessException(reason, sdcBatchStatusCodes, batchFile, persistStudentRecords);
    entity.setSdcSchoolCollectionStatusCode(sdcBatchStatusCodes.getCode());
    if (persistStudentRecords && batchFile != null) { // for certain business exception, system needs to store the student details as well.
      for (final var student : batchFile.getStudentDetails()) { // set the object so that PK/FK relationship will be auto established by hibernate.
        final var sdcSchoolStudentEntity = this.toSdcSchoolStudentEntity(student, entity);
        entity.getSDCSchoolStudentEntities().add(sdcSchoolStudentEntity);
      }
    }
    return entity;
  }

  @Override
  public SdcSchoolCollectionStudentEntity toSdcSchoolStudentEntity(final SdcStudentDetails studentDetails, final SdcSchoolCollectionEntity sdcSchoolBatchEntity) {
    final var entity = this.delegate.toSdcSchoolStudentEntity(studentDetails, sdcSchoolBatchEntity);
    entity.setSdcSchoolCollectionID(sdcSchoolBatchEntity.getSdcSchoolCollectionID()); // add thePK/FK relationship
    entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());

    entity.setPostalCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getPostalCode()));
    entity.setGender(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getGender()));
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
    entity.setNativeAncestryInd(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getNativeAncestryIndicator()));
    entity.setHomeLanguageSpokenCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getHomeSpokenLanguageCode()));
    entity.setOtherCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getOtherCourses()));
    entity.setSupportBlocks(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getSupportBlocks()));
    entity.setEnrolledProgramCodes(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getEnrolledProgramCodes()));
    entity.setCareerProgramCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getCareerProgramCode()));
    entity.setNumberOfCourses(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getNumberOfCourses()));
    entity.setBandCode(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getBandCode()));

    if(StringUtils.isNotBlank(studentDetails.getPen()) && studentDetails.getPen().length() == 9) {
      entity.setStudentPen(StringMapper.trimUppercaseAndScrubDiacriticalMarks(studentDetails.getPen()));
    }

    return entity;
  }

}
