package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.IndependentSchoolFundingGroupMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.IndependentSchoolFundingGroupRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class IndependentSchoolFundingGroupService {

  @Getter(AccessLevel.PRIVATE)
  private final IndependentSchoolFundingGroupRepository independentSchoolFundingGroupRepository;
  private static final IndependentSchoolFundingGroupMapper independentSchoolFundingGroupMapper = IndependentSchoolFundingGroupMapper.mapper;

  @Autowired
  public IndependentSchoolFundingGroupService(IndependentSchoolFundingGroupRepository independentSchoolFundingGroupRepository) {
    this.independentSchoolFundingGroupRepository = independentSchoolFundingGroupRepository;
  }

  public Optional<IndependentSchoolFundingGroupEntity> getIndependentSchoolFundingGroup(UUID schoolFundingGroupID) {
    return independentSchoolFundingGroupRepository.findById(schoolFundingGroupID);
  }

  public List<IndependentSchoolFundingGroupEntity> getIndependentSchoolFundingGroups(UUID schoolID) {
    return independentSchoolFundingGroupRepository.findAllBySchoolID(schoolID);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IndependentSchoolFundingGroupEntity createIndependentSchoolFundingGroup(IndependentSchoolFundingGroup independentSchoolFundingGroup) {
    IndependentSchoolFundingGroupEntity independentSchoolFundingGroupEntity = IndependentSchoolFundingGroupMapper.mapper.toModel(independentSchoolFundingGroup);
    TransformUtil.uppercaseFields(independentSchoolFundingGroupEntity);

    return independentSchoolFundingGroupRepository.save(independentSchoolFundingGroupEntity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void deleteIndependentSchoolFundingGroup(UUID schoolFundingGroupID) {
    Optional<IndependentSchoolFundingGroupEntity> entityOptional = independentSchoolFundingGroupRepository.findById(schoolFundingGroupID);
    IndependentSchoolFundingGroupEntity entity = entityOptional.orElseThrow(() -> new EntityNotFoundException(IndependentSchoolFundingGroupEntity.class, "schoolFundingGroupID", schoolFundingGroupID.toString()));
    independentSchoolFundingGroupRepository.delete(entity);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public IndependentSchoolFundingGroupEntity updateIndependentSchoolFundingGroup(IndependentSchoolFundingGroup independentSchoolFundingGroup) {
    Optional<IndependentSchoolFundingGroupEntity> independentSchoolFundingGroupEntity = independentSchoolFundingGroupRepository.findById(UUID.fromString(independentSchoolFundingGroup.getSchoolFundingGroupID()));

    if(independentSchoolFundingGroupEntity.isEmpty()) {
      throw new EntityNotFoundException(IndependentSchoolFundingGroupEntity.class, "IndependentSchoolFundingGroupEntity", independentSchoolFundingGroup.getSchoolFundingGroupID());
    }

    var curIndependentSchoolFundingGroupEntity = independentSchoolFundingGroupEntity.get();
    var incomingIndependentSchoolFundingGroupEntity = independentSchoolFundingGroupMapper.toModel(independentSchoolFundingGroup);
    BeanUtils.copyProperties(incomingIndependentSchoolFundingGroupEntity, curIndependentSchoolFundingGroupEntity, "schoolFundingGroupID, schoolID, createUser, createDate");
    TransformUtil.uppercaseFields(curIndependentSchoolFundingGroupEntity);

    return this.independentSchoolFundingGroupRepository.save(curIndependentSchoolFundingGroupEntity);
  }

}
