package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentService {

  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;

  public SdcSchoolCollectionStudentEntity getSdcSchoolCollectionStudent(
      UUID sdcSchoolCollectionStudentID) {

    Optional<SdcSchoolCollectionStudentEntity> sdcSchoolCollectionStudentEntityOptional = sdcSchoolCollectionStudentRepository.findById(sdcSchoolCollectionStudentID);
    SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity = sdcSchoolCollectionStudentEntityOptional.orElseThrow(() -> new EntityNotFoundException(
        SdcSchoolCollectionStudent.class, "sdcSchoolCollectionStudentId", sdcSchoolCollectionStudentID.toString()));

    return sdcSchoolCollectionStudentEntity;
  };

  @Transactional(propagation = Propagation.SUPPORTS)
  public CompletableFuture<Page<SdcSchoolCollectionStudentEntity>> findAll(final Specification<SdcSchoolCollectionStudentEntity> secureExchangeSpecs, final Integer pageNumber, final Integer pageSize, final List<Sort.Order> sorts) {
    final Pageable paging = PageRequest.of(pageNumber, pageSize, Sort.by(sorts));
    try {
      val result = this.sdcSchoolCollectionStudentRepository.findAll(secureExchangeSpecs, paging);
      return CompletableFuture.completedFuture(result);
    } catch (final Exception ex) {
      throw new CompletionException(ex);
    }
  }

}
