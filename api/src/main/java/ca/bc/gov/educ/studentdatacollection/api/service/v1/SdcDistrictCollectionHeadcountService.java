package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.helpers.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
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
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getGradeEnrollmentHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    enrollmentHeadcountHelper.setGradeCodesForDistricts();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);
    List<EnrollmentHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertEnrollmentBySchoolHeadcountResults(collectionRawData);
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
    collectionData = frenchCombinedHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(collectionRawData);

    if (compare) {
      frenchCombinedHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    specialEdHeadcountHelper.setGradeCodesForDistricts();

    List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionDataForHeadcounts = specialEdHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(collectionRawDataForHeadcount);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    if (compare) {
      specialEdHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionDataForHeadcounts);
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

    HeadcountResultsTable collectionData = careerHeadcountHelper.convertCareerBySchoolHeadcountResults(collectionRawData);
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

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getIndigenousHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    indigenousHeadcountHelper.setGradeCodesForDistricts();

    List<IndigenousHeadcountResult> result = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);
    HeadcountResultsTable headcountResultsTable = indigenousHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(result);
    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getBandResidenceHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    List<BandResidenceHeadcountResult> result = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    bandResidenceHeadcountHelper.setBandTitles(result);

    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    HeadcountResultsTable headcountResultsTable = bandResidenceHeadcountHelper.convertBandHeadcountResults(result);

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getBandResidenceHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    List<BandResidenceHeadcountResult> result = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(sdcDistrictCollectionID);
    bandResidenceHeadcountHelper.setSchoolTitles(result);

    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    HeadcountResultsTable headcountResultsTable = bandResidenceHeadcountHelper.convertBandHeadcountResults(result);

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getEllHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    ellHeadcountHelper.setGradeCodesForDistricts();
    ellHeadcountHelper.setDistrictReportTitles();

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
    HeadcountResultsTable collectionData = ellHeadcountHelper.convertEllBySchoolHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);
    if (compare) {
      ellHeadcountHelper.setComparisonValuesForDistrictBySchool(sdcDistrictCollectionEntity, headcountHeaderList, collectionData);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

}
