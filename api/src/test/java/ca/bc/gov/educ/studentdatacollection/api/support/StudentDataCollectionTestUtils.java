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
  SdcRepository collectionRepository;
  @Autowired
  CollectionCodeRepository collectionCodeRepository;
  @Autowired
  CollectionCodeCriteriaRepository collectionCodeCriteriaRepository;
  @Autowired
  SdcSchoolBatchRepository sdcSchoolRepository;
  @Autowired
  SdcSchoolHistoryRepository sdcSchoolHistoryRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void cleanDB() {
    this.sdcSchoolHistoryRepository.deleteAll();
    this.sdcSchoolRepository.deleteAll();
    this.collectionRepository.deleteAll();
    this.collectionCodeCriteriaRepository.deleteAll();
    this.collectionCodeRepository.deleteAll();

  }
}
