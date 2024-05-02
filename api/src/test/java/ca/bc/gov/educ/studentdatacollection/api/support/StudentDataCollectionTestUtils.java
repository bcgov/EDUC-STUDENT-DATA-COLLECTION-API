package ca.bc.gov.educ.studentdatacollection.api.support;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("test")
public class StudentDataCollectionTestUtils {

  @Autowired
  CollectionRepository collectionRepository;
  @Autowired
  CollectionTypeCodeRepository collectionCodeRepository;
  @Autowired
  CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
  @Autowired
  SdcSchoolCollectionRepository sdcSchoolRepository;
  @Autowired
  SdcSchoolCollectionHistoryRepository sdcSchoolHistoryRepository;
  @Autowired
  SdcDuplicateRepository sdcDuplicateRepository;
  @Autowired
  EnrolledProgramCodeRepository enrolledProgramCodeRepository;
  @Autowired
  CareerProgramCodeRepository careerProgramCodeRepository;
  @Autowired
  HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository;
  @Autowired
  BandCodeRepository bandCodeRepository;
  @Autowired
  FundingCodeRepository fundingCodeRepository;
  @Autowired
  EnrolledGradeCodeRepository enrolledGradeCodeRepository;
  @Autowired
  GenderCodeRepository genderCodeRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    sdcDuplicateRepository.deleteAll();
    sdcSchoolHistoryRepository.deleteAll();
    sdcSchoolRepository.deleteAll();
    collectionRepository.deleteAll();
    collectionCodeCriteriaRepository.deleteAll();
    collectionCodeRepository.deleteAll();

    enrolledProgramCodeRepository.deleteAll();
    careerProgramCodeRepository.deleteAll();
    homeLanguageSpokenCodeRepository.deleteAll();
    bandCodeRepository.deleteAll();
    fundingCodeRepository.deleteAll();
    enrolledGradeCodeRepository.deleteAll();
    genderCodeRepository.deleteAll();
  }

}
