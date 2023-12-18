package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.helpers.CareerHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.EnrollmentHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.FrenchHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
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
  private final CareerHeadcountHelper careerHeadcountHelper;

  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolId(sdcSchoolCollectionID);
    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionData), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionData));
    if(compare) {
      enrollmentHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    enrollmentHeadcountHelper.stripZeroColumns(headcountHeaderList.get(1));
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<FrenchHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(sdcSchoolCollectionID);
    HeadcountResultsTable collectionData = frenchHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = frenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
    if(compare) {
      frenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getCareerHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<CareerHeadcountResult> result = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySchoolId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = careerHeadcountHelper.convertHeadcountResults(result);
    List<HeadcountHeader> headcountHeaderList = careerHeadcountHelper.getHeaders(sdcSchoolCollectionID);
    if(compare) {
      careerHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }
}
