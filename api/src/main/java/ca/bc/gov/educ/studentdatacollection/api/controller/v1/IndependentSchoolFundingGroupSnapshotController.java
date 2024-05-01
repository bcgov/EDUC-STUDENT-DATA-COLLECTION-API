package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.IndependentSchoolFundingGroupSnapshotEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.IndependentSchoolFundingGroupSnapshotMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupSnapshotEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.IndependentSchoolFundingGroupSnapshotService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroupSnapshot;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@RestController
@Slf4j
public class IndependentSchoolFundingGroupSnapshotController implements IndependentSchoolFundingGroupSnapshotEndpoint {

  private static final IndependentSchoolFundingGroupSnapshotMapper independentSchoolFundingGroupMapper = IndependentSchoolFundingGroupSnapshotMapper.mapper;
  private final IndependentSchoolFundingGroupSnapshotService independentSchoolFundingGroupSnapshotService;

  @Autowired
  public IndependentSchoolFundingGroupSnapshotController(IndependentSchoolFundingGroupSnapshotService independentSchoolFundingGroupSnapshotService) {
    this.independentSchoolFundingGroupSnapshotService = independentSchoolFundingGroupSnapshotService;
  }

  @Override
  public List<IndependentSchoolFundingGroupSnapshot> getIndependentSchoolFundingGroupSnapshot(UUID schoolID, UUID collectionID) {
    List<IndependentSchoolFundingGroupSnapshotEntity> independentSchoolFundingGroupSnapshot = this.independentSchoolFundingGroupSnapshotService.getIndependentSchoolFundingGroupSnapshot(schoolID, collectionID);

    List<IndependentSchoolFundingGroupSnapshot> independentSchoolFundingGroupList = new ArrayList<>();
    for(IndependentSchoolFundingGroupSnapshotEntity entity: independentSchoolFundingGroupSnapshot){
      independentSchoolFundingGroupList.add(independentSchoolFundingGroupMapper.toStructure(entity));
    }

    return independentSchoolFundingGroupList;
  }


}
