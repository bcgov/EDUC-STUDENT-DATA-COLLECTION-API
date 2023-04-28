package ca.bc.gov.educ.studentdatacollection.api.repository.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.HomeLanguageSpokenCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HomeLanguageSpokenCodeRepository extends JpaRepository<HomeLanguageSpokenCodeEntity, String> {
}
