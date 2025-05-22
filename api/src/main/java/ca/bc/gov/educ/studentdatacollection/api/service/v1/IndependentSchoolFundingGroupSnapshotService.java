package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupSnapshotEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupSnapshotRepository;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class IndependentSchoolFundingGroupSnapshotService {

  @Getter(AccessLevel.PRIVATE)
  private final IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository;

  @Autowired
  public IndependentSchoolFundingGroupSnapshotService(IndependentSchoolFundingGroupSnapshotRepository independentSchoolFundingGroupSnapshotRepository) {
    this.independentSchoolFundingGroupSnapshotRepository = independentSchoolFundingGroupSnapshotRepository;
  }

  public List<IndependentSchoolFundingGroupSnapshotEntity> getIndependentSchoolFundingGroupSnapshot(UUID schoolID, UUID collectionID) {
    return independentSchoolFundingGroupSnapshotRepository.findAllBySchoolIDAndAndCollectionID(schoolID, collectionID);
  }

  public List<IndependentSchoolFundingGroupSnapshotEntity> getAllIndependentSchoolFundingGroupSnapshot(UUID collectionID) {
    return independentSchoolFundingGroupSnapshotRepository.findAllByCollectionID(collectionID);
  }

}
