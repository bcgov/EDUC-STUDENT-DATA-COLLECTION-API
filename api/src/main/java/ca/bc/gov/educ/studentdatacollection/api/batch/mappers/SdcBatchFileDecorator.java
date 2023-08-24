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
    entity.setSdcSchoolCollectionID(UUID.fromString(sdcSchoolCollectionID));
    entity.setUploadFileName(upload.getFileName());
    entity.setUploadReportDate(file.getBatchFileHeader().getReportDate());
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
    entity.setSdcSchoolCollection(sdcSchoolBatchEntity); // add thePK/FK relationship
    entity.setSdcSchoolCollectionStudentStatusCode(SdcSchoolCollectionStatus.LOADED.getCode());

    entity.setStudentPen(StringMapper.trimAndUppercase(studentDetails.getPen()));
    entity.setPostalCode(StringMapper.trimAndUppercase(studentDetails.getPostalCode()));
    entity.setGender(StringMapper.trimAndUppercase(studentDetails.getGender()));
    entity.setDob(StringMapper.trimAndUppercase(studentDetails.getBirthDate()));
    entity.setEnrolledGradeCode(StringMapper.trimAndUppercase(studentDetails.getEnrolledGradeCode()));
    entity.setLegalLastName(StringMapper.trimAndUppercase(studentDetails.getLegalSurname()));
    entity.setLegalFirstName(StringMapper.trimAndUppercase(studentDetails.getLegalGivenName()));
    entity.setLegalMiddleNames(StringMapper.trimAndUppercase(studentDetails.getLegalMiddleName()));
    entity.setUsualLastName(StringMapper.trimAndUppercase(studentDetails.getUsualSurname()));
    entity.setUsualFirstName(StringMapper.trimAndUppercase(studentDetails.getUsualGivenName()));
    entity.setUsualMiddleNames(StringMapper.trimAndUppercase(studentDetails.getUsualMiddleName()));
    entity.setLocalID(StringMapper.trimAndUppercase(studentDetails.getLocalStudentID()));
    entity.setSpecialEducationCategoryCode(StringMapper.trimAndUppercase(studentDetails.getSpecialEducationCategory()));
    entity.setSchoolFundingCode(StringMapper.trimAndUppercase(studentDetails.getSchoolFundingCode()));
    entity.setNativeAncestryInd(StringMapper.trimAndUppercase(studentDetails.getNativeAncestryIndicator()));
    entity.setHomeLanguageSpokenCode(StringMapper.trimAndUppercase(studentDetails.getHomeSpokenLanguageCode()));
    entity.setOtherCourses(StringMapper.trimAndUppercase(studentDetails.getOtherCourses()));
    entity.setSupportBlocks(StringMapper.trimAndUppercase(studentDetails.getSupportBlocks()));
    entity.setEnrolledProgramCodes(StringMapper.trimAndUppercase(studentDetails.getEnrolledProgramCodes()));
    entity.setCareerProgramCode(StringMapper.trimAndUppercase(studentDetails.getCareerProgramCode()));
    entity.setNumberOfCourses(StringMapper.trimAndUppercase(studentDetails.getNumberOfCourses()));
    entity.setBandCode(StringMapper.trimAndUppercase(studentDetails.getBandCode()));

    if(StringUtils.isNotBlank(studentDetails.getPen()) && studentDetails.getPen().length() == 9) {
      entity.setStudentPen(StringMapper.trimAndUppercase(studentDetails.getPen()));
    }

    return entity;
  }

}
