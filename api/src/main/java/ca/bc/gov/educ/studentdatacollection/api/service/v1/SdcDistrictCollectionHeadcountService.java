package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcDistrictCollectionHeadcountService {
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final EnrollmentHeadcountHelper enrollmentHeadcountHelper;
  private final SpecialEdHeadcountHelper specialEdHeadcountHelper;
  private final CareerHeadcountHelper careerHeadcountHelper;
  private final FrenchCombinedHeadcountHelper frenchCombinedHeadcountHelper;
  private final IndigenousHeadcountHelper indigenousHeadcountHelper;
  private final BandResidenceHeadcountHelper bandResidenceHeadcountHelper;
  private final EllHeadcountHelper ellHeadcountHelper;
  private final RefugeeHeadcountHelper refugeeHeadcountHelper;
  private final SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    enrollmentHeadcountHelper.setGradeCodesForDistricts();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionData), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionData));

    if (compare) {
      enrollmentHeadcountHelper.setComparisonValuesForDistrictReporting(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }

    enrollmentHeadcountHelper.stripZeroColumns(headcountHeaderList.get(1));
    enrollmentHeadcountHelper.stripPreSchoolSection(collectionData);
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getGradeEnrollmentHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    enrollmentHeadcountHelper.setGradeCodesForDistricts();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);
    List<EnrollmentHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertEnrollmentBySchoolHeadcountResults(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawData);
    HeadcountResultsTable collectionDataForHeadcounts = enrollmentHeadcountHelper.convertHeadcountResults(collectionRawDataForHeadcount);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionDataForHeadcounts), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionDataForHeadcounts));

    if (compare) {
      enrollmentHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    enrollmentHeadcountHelper.stripZeroColumns(headcountHeaderList.get(1));
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    specialEdHeadcountHelper.setGradeCodesForDistricts();

    List<SpecialEdHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionData = specialEdHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if (compare) {
      specialEdHeadcountHelper.setComparisonValuesForDistrictReporting(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdVarianceHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity) {
    specialEdHeadcountHelper.setGradeCodesForDistricts();

    //TODO
    // from district collection entity we need to make sure it is Feb collection or find the previous feb collection
    // from feb collection we need to find the previous sept collection
    // from these two collections we need to pull our raw data
    // from this raw data we should be able to find the variance
    // lay it all out nicely

    // Get previous February collection relative to given collectionID
    SdcDistrictCollectionEntity febCollection = sdcDistrictCollectionRepository.findLastOrCurrentSdcDistrictCollectionByCollectionType(CollectionTypeCodes.FEBRUARY.getTypeCode(), sdcDistrictCollectionEntity.getSdcDistrictCollectionID())
            .orElseThrow(() -> new RuntimeException("No previous or current February sdc district collection found."));

    // Get previous September collection relative to previous February collection
    SdcDistrictCollectionEntity septCollection = sdcDistrictCollectionRepository.findLastSdcDistrictCollectionByCollectionTypeBefore(CollectionTypeCodes.SEPTEMBER.getTypeCode(), febCollection.getSdcDistrictCollectionID())
            .orElseThrow(() -> new RuntimeException("No previous September sdc district collection found relative to the February collection."));

    List<SpecialEdHeadcountResult> febCollectionRawData = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcDistrictCollectionId(febCollection.getSdcDistrictCollectionID());

    HeadcountResultsTable collectionData = specialEdHeadcountHelper.convertHeadcountResults(febCollectionRawData);

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    frenchCombinedHeadcountHelper.setGradeCodesForDistricts();

    List<HeadcountHeader> headcountHeaderList;
    HeadcountResultsTable collectionData;
    List<FrenchCombinedHeadcountResult> collectionRawData;

    collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    headcountHeaderList = frenchCombinedHeadcountHelper.getHeaders(sdcDistrictCollectionID);
    collectionData = frenchCombinedHeadcountHelper.convertHeadcountResults(collectionRawData);

    if (compare) {
      frenchCombinedHeadcountHelper.setComparisonValuesForDistrictReporting(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    frenchCombinedHeadcountHelper.setGradeCodesForDistricts();

    List<HeadcountHeader> headcountHeaderList;
    HeadcountResultsTable collectionData;
    List<FrenchCombinedHeadcountResult> collectionRawData;

    collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);
    headcountHeaderList = frenchCombinedHeadcountHelper.getHeaders(sdcDistrictCollectionID);
    collectionData = frenchCombinedHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawData);

    if (compare) {
      frenchCombinedHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    specialEdHeadcountHelper.setGradeCodesForDistricts();

    List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionDataForHeadcounts = specialEdHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if (compare) {
      specialEdHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionDataForHeadcounts, false);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionDataForHeadcounts).build();
  }

  public SdcSchoolCollectionStudentHeadcounts   getSpecialEdCatHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();

    List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdCategoryBySchoolIdAndSdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionDataForHeadcounts = specialEdHeadcountHelper.convertHeadcountResultsToSpecialEdCategoryTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawDataForHeadcount);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if (compare) {
      specialEdHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionDataForHeadcounts, true);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionDataForHeadcounts).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getCareerHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();

    List<CareerHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    HeadcountResultsTable collectionData = careerHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = careerHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if(compare) {
      careerHeadcountHelper.setComparisonValuesForDistrictReporting(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getCareerPerSchoolHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();

    List<CareerHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionData = careerHeadcountHelper.convertCareerBySchoolHeadcountResults(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), collectionRawData);
    List<HeadcountHeader> headcountHeaderList = careerHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if(compare) {
      careerHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getIndigenousHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    indigenousHeadcountHelper.setGradeCodesForDistricts();

    List<IndigenousHeadcountResult> result = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    HeadcountResultsTable headcountResultsTable = indigenousHeadcountHelper.convertHeadcountResults(result);
    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if(compare) {
      indigenousHeadcountHelper.setComparisonValuesForDistrict(sdcDistrictCollectionEntity, headcountHeaderList);
      indigenousHeadcountHelper.setResultsTableComparisonValuesForDistrict(sdcDistrictCollectionEntity, headcountResultsTable);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getIndigenousHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    indigenousHeadcountHelper.setGradeCodesForDistricts();

    List<IndigenousHeadcountResult> result = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);
    HeadcountResultsTable headcountResultsTable = indigenousHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(sdcDistrictCollectionEntity.getSdcDistrictCollectionID(), result);
    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if (compare) {
      indigenousHeadcountHelper.setComparisonValuesForDistrict(sdcDistrictCollectionEntity, headcountHeaderList);
      indigenousHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountResultsTable);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getBandResidenceHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    List<BandResidenceHeadcountResult> result = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);

    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    HeadcountResultsTable headcountResultsTable = bandResidenceHeadcountHelper.convertBandHeadcountResults(result, false, null);

    if(compare) {
      indigenousHeadcountHelper.setComparisonValuesForDistrict(sdcDistrictCollectionEntity, headcountHeaderList);
      bandResidenceHeadcountHelper.setBandResultsTableComparisonValuesDistrict(sdcDistrictCollectionEntity, headcountResultsTable, false);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getBandResidenceHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    List<BandResidenceHeadcountResult> result = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);

    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    HeadcountResultsTable headcountResultsTable = bandResidenceHeadcountHelper.convertBandHeadcountResults(result, true, sdcDistrictCollectionID);

    if(compare) {
      indigenousHeadcountHelper.setComparisonValuesForDistrict(sdcDistrictCollectionEntity, headcountHeaderList);
      bandResidenceHeadcountHelper.setBandResultsTableComparisonValuesDistrict(sdcDistrictCollectionEntity, headcountResultsTable, true);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getEllHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    ellHeadcountHelper.setGradeCodesForDistricts();

    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    HeadcountResultsTable collectionData = ellHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    if (compare) {
      ellHeadcountHelper.setComparisonValuesForDistrictReporting(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }


  public SdcSchoolCollectionStudentHeadcounts getEllPerSchoolHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    ellHeadcountHelper.setGradeCodesForDistricts();

    List<EllHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEllHeadcountsByBySchoolIdAndSdcDistrictCollectionId(sdcDistrictCollectionID);
    HeadcountResultsTable collectionData = ellHeadcountHelper.convertEllBySchoolHeadcountResults(sdcDistrictCollectionID, collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    if (compare) {
      ellHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getRefugeePerSchoolHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    List<RefugeeHeadcountResult> result = sdcSchoolCollectionStudentRepository.getRefugeeHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);

    List<HeadcountHeader> headcountHeaderList = refugeeHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    HeadcountResultsTable headcountResultsTable = refugeeHeadcountHelper.convertRefugeeHeadcountResults(sdcDistrictCollectionID, result);

    if (compare) {
      log.info("compare block refugee per school headcount");
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

}
