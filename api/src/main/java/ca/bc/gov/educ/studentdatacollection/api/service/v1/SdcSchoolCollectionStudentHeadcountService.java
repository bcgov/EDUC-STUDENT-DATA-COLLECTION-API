package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.helpers.EllHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.EnrollmentHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.FrenchHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentHeadcountService {
  private final SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository;
  private final EnrollmentHeadcountHelper enrollmentHeadcountHelper;
  private final FrenchHeadcountHelper frenchHeadcountHelper;
  private final EllHeadcountHelper ellHeadcountHelper;


  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolId(sdcSchoolCollectionID);
    List<HeadcountTableData> collectionData = enrollmentHeadcountHelper.convertEnrollmentHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionData), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionData));
    if (compare) {
      enrollmentHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    enrollmentHeadcountHelper.stripZeroColumns(headcountHeaderList.get(1));
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountTableDataList(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<FrenchHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(sdcSchoolCollectionID);
    List<HeadcountTableData> collectionData = frenchHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = frenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
    if (compare) {
      frenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountTableDataList(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getEllHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<EllHeadcountResult> collectionRawData =
      sdcSchoolCollectionStudentRepository.getEllHeadcountsBySchoolId(sdcSchoolCollectionID);
    List<HeadcountTableData> collectionData = ellHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcSchoolCollectionID);
    if (compare) {
      ellHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder()
      .headcountHeaders(headcountHeaderList).headcountTableDataList(collectionData).build();
  }
}
