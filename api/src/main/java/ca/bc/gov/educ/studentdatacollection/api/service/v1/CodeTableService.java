package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class CodeTableService {
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final EnrolledProgramCodeRepository enrolledProgramCodeRepository;
  private final CareerProgramCodeRepository careerProgramCodeRepository;
  private final HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository;
  private final BandCodeRepository bandCodeRepository;
  private final FundingCodeRepository fundingCodeRepository;
  private final EnrolledGradeCodeRepository enrolledGradeCodeRepository;
  private final SpecialEducationCategoryCodeRepository specialEducationCategoryCodeRepository;

  /**
   * Instantiates a new Code table service.
   *
   * @param collectionCodeRepository
   * @param enrolledProgramCodeRepository
   * @param careerProgramCodeRepository
   * @param homeLanguageSpokenCodeRepository
   * @param bandCodeRepository
   * @param fundingCodeRepository
   * @param enrolledGradeCodeRepository
   * @param specialEducationCategoryCodeRepository
   */
  @Autowired
  public CodeTableService(CollectionTypeCodeRepository collectionCodeRepository, EnrolledProgramCodeRepository enrolledProgramCodeRepository, CareerProgramCodeRepository careerProgramCodeRepository, HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository, BandCodeRepository bandCodeRepository, FundingCodeRepository fundingCodeRepository, EnrolledGradeCodeRepository enrolledGradeCodeRepository, SpecialEducationCategoryCodeRepository specialEducationCategoryCodeRepository) {
    this.collectionCodeRepository = collectionCodeRepository;
    this.enrolledProgramCodeRepository = enrolledProgramCodeRepository;
    this.careerProgramCodeRepository = careerProgramCodeRepository;
    this.homeLanguageSpokenCodeRepository = homeLanguageSpokenCodeRepository;
    this.bandCodeRepository = bandCodeRepository;
    this.fundingCodeRepository = fundingCodeRepository;
    this.enrolledGradeCodeRepository = enrolledGradeCodeRepository;
    this.specialEducationCategoryCodeRepository = specialEducationCategoryCodeRepository;
  }

  @Cacheable("enrolledProgramCodes")
  public List<EnrolledProgramCodeEntity> getAllEnrolledProgramCodes() {
    return enrolledProgramCodeRepository.findAll();
  }

  @Cacheable("careerProgramCodes")
  public List<CareerProgramCodeEntity> getAllCareerProgramCodes() {
    return careerProgramCodeRepository.findAll();
  }

  @Cacheable("homeLanguageSpokenCodes")
  public List<HomeLanguageSpokenCodeEntity> getAllHomeLanguageSpokenCodes() {
    return homeLanguageSpokenCodeRepository.findAll();
  }

  @Cacheable("bandCodes")
  public List<BandCodeEntity> getAllBandCodes() {
    return bandCodeRepository.findAll();
  }

  @Cacheable("fundingCodes")
  public List<SchoolFundingCodeEntity> getAllFundingCodes() {
    return fundingCodeRepository.findAll();
  }

  @Cacheable("enrolledGradeCodes")
  public List<EnrolledGradeCodeEntity> getAllGradeCodes() {
    return enrolledGradeCodeRepository.findAll();
  }
  @Cacheable("specialEducationCategoryCodes")
  public List<SpecialEducationCategoryCodeEntity> getAllSpecialEducationCategoryCodes() {
    return specialEducationCategoryCodeRepository.findAll();
  }
  @Cacheable("collectionCodes")
  public List<CollectionTypeCodeEntity> getCollectionCodeList() {
    return collectionCodeRepository.findAll();
  }
  public Optional<CollectionTypeCodeEntity> getCollectionCode(String collectionCode) {
    return collectionCodeRepository.findById(collectionCode);
  }


}
