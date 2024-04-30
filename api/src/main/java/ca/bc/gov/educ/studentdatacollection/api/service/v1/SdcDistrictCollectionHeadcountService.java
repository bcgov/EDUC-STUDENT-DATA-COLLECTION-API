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
  private final FrenchHeadcountHelper frenchHeadcountHelper;

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

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }
  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcounts(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    frenchHeadcountHelper.setGradeCodesForDistricts();

    List<HeadcountHeader> headcountHeaderList;
    HeadcountResultsTable collectionData;

    List<FrenchHeadcountResult> collectionRawData;
    collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcDistrictCollectionId(sdcDistrictCollectionID);
    frenchHeadcountHelper.setDistrict(true);
    headcountHeaderList = frenchHeadcountHelper.getHeaders(sdcDistrictCollectionID);
    collectionData = frenchHeadcountHelper.convertHeadcountResults(collectionRawData);

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdHeadcountsPerSchool(SdcDistrictCollectionEntity sdcDistrictCollectionEntity, boolean compare) {
    var sdcDistrictCollectionID = sdcDistrictCollectionEntity.getSdcDistrictCollectionID();
    specialEdHeadcountHelper.setGradeCodesForDistricts();

    List<SpecialEdHeadcountResult> collectionRawDataForHeadcount = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySchoolIdAndBySdcDistrictCollectionId(sdcDistrictCollectionID);

    HeadcountResultsTable collectionDataForHeadcounts = specialEdHeadcountHelper.convertHeadcountResultsToSchoolGradeTable(collectionRawDataForHeadcount);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcDistrictCollectionID, true);

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

}
