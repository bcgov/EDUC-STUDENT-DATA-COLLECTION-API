package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SpecialEducationCategoryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpecialEducationCategoryRepository extends JpaRepository<SpecialEducationCategoryCodeEntity, String> {
}
