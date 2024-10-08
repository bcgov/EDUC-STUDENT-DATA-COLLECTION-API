package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.CodeTableMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BandCode;
import ca.bc.gov.educ.studentdatacollection.api.util.TransformUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CodeTableService {
  private final CollectionTypeCodeRepository collectionCodeRepository;
  private final EnrolledProgramCodeRepository enrolledProgramCodeRepository;
  private final CareerProgramCodeRepository careerProgramCodeRepository;
  private final HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository;
  private final BandCodeRepository bandCodeRepository;
  private final FundingCodeRepository fundingCodeRepository;
  private final EnrolledGradeCodeRepository enrolledGradeCodeRepository;
  private final SpecialEducationCategoryRepository specialEducationCategoryRepository;
  private final GenderCodeRepository genderCodeRepository;
  private final SchoolGradeCodeRepository schoolGradeCodeRepository;
  private final SchoolFundingGroupCodeRepository schoolFundingGroupCodeRepository;
  private final ProgramDuplicateTypeCodeRepository programDuplicateTypeCodeRepository;
  private final SdcSchoolCollectionStatusCodeRepository sdcSchoolCollectionStatusCodeRepository;
  private final SdcDistrictCollectionStatusCodeRepository sdcDistrictCollectionStatusCodeRepository;

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
   * @param specialEducationCategoryRepository
   * @param genderCodeRepository
   * @param schoolGradeCodeRepository
   * @param schoolFundingGroupCodeRepository
   * @param sdcSchoolCollectionStatusCodeRepository
   * @param sdcDistrictCollectionStatusCodeRepository
   */
  @Autowired
  public CodeTableService(CollectionTypeCodeRepository collectionCodeRepository, EnrolledProgramCodeRepository enrolledProgramCodeRepository, CareerProgramCodeRepository careerProgramCodeRepository, HomeLanguageSpokenCodeRepository homeLanguageSpokenCodeRepository, BandCodeRepository bandCodeRepository, FundingCodeRepository fundingCodeRepository, EnrolledGradeCodeRepository enrolledGradeCodeRepository, SpecialEducationCategoryRepository specialEducationCategoryRepository,
                          GenderCodeRepository genderCodeRepository, SchoolGradeCodeRepository schoolGradeCodeRepository, SchoolFundingGroupCodeRepository schoolFundingGroupCodeRepository, ProgramDuplicateTypeCodeRepository programDuplicateTypeCodeRepository, SdcSchoolCollectionStatusCodeRepository sdcSchoolCollectionStatusCodeRepository, SdcDistrictCollectionStatusCodeRepository sdcDistrictCollectionStatusCodeRepository) {
    this.collectionCodeRepository = collectionCodeRepository;
    this.enrolledProgramCodeRepository = enrolledProgramCodeRepository;
    this.careerProgramCodeRepository = careerProgramCodeRepository;
    this.homeLanguageSpokenCodeRepository = homeLanguageSpokenCodeRepository;
    this.bandCodeRepository = bandCodeRepository;
    this.fundingCodeRepository = fundingCodeRepository;
    this.enrolledGradeCodeRepository = enrolledGradeCodeRepository;
    this.specialEducationCategoryRepository = specialEducationCategoryRepository;
    this.genderCodeRepository = genderCodeRepository;
    this.schoolGradeCodeRepository = schoolGradeCodeRepository;
    this.schoolFundingGroupCodeRepository = schoolFundingGroupCodeRepository;
    this.programDuplicateTypeCodeRepository = programDuplicateTypeCodeRepository;
    this.sdcSchoolCollectionStatusCodeRepository = sdcSchoolCollectionStatusCodeRepository;
    this.sdcDistrictCollectionStatusCodeRepository = sdcDistrictCollectionStatusCodeRepository;
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
  public List<EnrolledGradeCodeEntity> getAllEnrolledGradeCodes() {
    return enrolledGradeCodeRepository.findAll();
  }
  @Cacheable("specialEducationCategoryCodes")
  public List<SpecialEducationCategoryCodeEntity> getAllSpecialEducationCategoryCodes() {
    return specialEducationCategoryRepository.findAll();
  }

  @Cacheable("genderCodes")
  public List<GenderCodeEntity> getAllGenderCodes() {
    return genderCodeRepository.findAll();
  }

  @Cacheable("collectionCodes")
  public List<CollectionTypeCodeEntity> getCollectionCodeList() {
    return collectionCodeRepository.findAll();
  }

  @Cacheable("schoolGradeCodes")
  public List<SchoolGradeCodeEntity> getAllSchoolGradeCodes() {
    return schoolGradeCodeRepository.findAll();
  }

  @Cacheable("schoolFundingGroupCodes")
  public List<SchoolFundingGroupCodeEntity> getAllSchoolFundingGroupCodes() {
    return schoolFundingGroupCodeRepository.findAll();
  }

  public Optional<CollectionTypeCodeEntity> getCollectionCode(String collectionCode) {
    return collectionCodeRepository.findById(collectionCode);
  }

  @Cacheable("programDuplicateTypeCodes")
  public List<ProgramDuplicateTypeCodeEntity> getProgramDuplicateTypeCodes() {
    return programDuplicateTypeCodeRepository.findAll();
  }

  @Cacheable("schoolCollectionStatusCodes")
  public List<SdcSchoolCollectionStatusCodeEntity> getSdcSchoolCollectionStatusCodesList() {
    return sdcSchoolCollectionStatusCodeRepository.findAll();
  }

  @Cacheable("districtCollectionStatusCodes")
  public List<SdcDistrictCollectionStatusCodeEntity> getSdcDistrictCollectionStatusCodesList() {
    return sdcDistrictCollectionStatusCodeRepository.findAll();
  }

  @CacheEvict(cacheNames="bandCodes", allEntries=true)
  public BandCode updateBandCode(BandCode bandCode) {
    var bandCodeEntityOpt = bandCodeRepository.findById(bandCode.getBandCode());
    BandCodeEntity entity = bandCodeEntityOpt.orElseThrow(() -> new EntityNotFoundException(BandCodeEntity.class, "bandCode", bandCode.getBandCode()));
    entity.setLabel(bandCode.getLabel());
    entity.setDescription(bandCode.getDescription());
    entity.setUpdateUser(bandCode.getUpdateUser());
    entity.setUpdateDate(LocalDateTime.now());
    TransformUtil.uppercaseFields(entity);
    var savedBand = bandCodeRepository.save(entity);
    return CodeTableMapper.mapper.toStructure(savedBand);
  }
}
