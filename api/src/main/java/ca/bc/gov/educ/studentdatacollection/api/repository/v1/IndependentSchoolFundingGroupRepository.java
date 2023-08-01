package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.IndependentSchoolFundingGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IndependentSchoolFundingGroupRepository extends JpaRepository<IndependentSchoolFundingGroupEntity, UUID> {

    List<IndependentSchoolFundingGroupEntity> findAllBySchoolID(UUID schoolID);
}
