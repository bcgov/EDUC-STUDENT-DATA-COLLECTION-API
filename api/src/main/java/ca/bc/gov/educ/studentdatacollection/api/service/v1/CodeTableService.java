package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionCodeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CodeTableService {
  private final CollectionCodeRepository collectionCodeRepository;

  /**
   * Instantiates a new Code table service.
   *
   * @param collectionCodeRepository
   */
  @Autowired
  public CodeTableService(CollectionCodeRepository collectionCodeRepository) {
    this.collectionCodeRepository = collectionCodeRepository;
  }

  @Cacheable("collectionCodes")
  public List<CollectionCodeEntity> getCollectionCodeList() {
    return collectionCodeRepository.findAll();
  }

  public Optional<CollectionCodeEntity> getCollectionCode(String collectionCode) {
    return collectionCodeRepository.findById(collectionCode);
  }


}
