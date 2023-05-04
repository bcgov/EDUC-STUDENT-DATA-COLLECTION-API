package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentEnrolledProgramMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  private static final SdcSchoolCollectionStudentEnrolledProgramMapper sdcSchoolCollectionStudentEnrolledProgramMapper = SdcSchoolCollectionStudentEnrolledProgramMapper.mapper;

  public SdcSchoolCollectionStudentEntity getSdcSchoolCollectionStudent(UUID sdcSchoolCollectionStudentID) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    return sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void writeEnrolledProgramCodes(UUID sdcSchoolCollectionStudentID, List<String> enrolledProgramCodes) {
    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);

    var student = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() ->
            new EntityNotFoundException(SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));

    enrolledProgramCodes.forEach(enrolledProgramCode -> {
      var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
      enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(student);
      enrolledProgramEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
      enrolledProgramEntity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setCreateDate(LocalDateTime.now());
      enrolledProgramEntity.setEnrolledProgramCode(enrolledProgramCode);

      student.getSdcStudentEnrolledProgramEntities().add(enrolledProgramEntity);
    });

    sdcSchoolCollectionStudentRepository.save(student);
  }

}
