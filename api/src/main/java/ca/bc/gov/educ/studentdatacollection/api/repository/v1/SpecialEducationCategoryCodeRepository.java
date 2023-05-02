package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SpecialEducationCategoryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialEducationCategoryCodeRepository extends JpaRepository<SpecialEducationCategoryCodeEntity, String> {
}
