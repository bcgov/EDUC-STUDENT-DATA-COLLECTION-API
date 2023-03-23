package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionTypeCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionTypeCodeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CodeTableService {
  private final CollectionTypeCodeRepository collectionCodeRepository;

  /**
   * Instantiates a new Code table service.
   *
   * @param collectionCodeRepository
   */
  @Autowired
  public CodeTableService(CollectionTypeCodeRepository collectionCodeRepository) {
    this.collectionCodeRepository = collectionCodeRepository;
  }

  @Cacheable("collectionCodes")
  public List<CollectionTypeCodeEntity> getCollectionCodeList() {
    return collectionCodeRepository.findAll();
  }

  public Optional<CollectionTypeCodeEntity> getCollectionCode(String collectionCode) {
    return collectionCodeRepository.findById(collectionCode);
  }


}
