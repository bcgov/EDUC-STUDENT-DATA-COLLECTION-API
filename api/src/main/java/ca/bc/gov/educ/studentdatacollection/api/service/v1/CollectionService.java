package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.CollectionRepository;

import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CollectionService {

  @Getter(AccessLevel.PRIVATE)
  private final CollectionRepository collectionRepository;

  @Autowired
  public CollectionService(CollectionRepository collectionRepository) {
    this.collectionRepository = collectionRepository;
  }

}
