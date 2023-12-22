package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.CareerHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.CsfFrenchHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.EllHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.EnrollmentHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.helpers.FrenchHeadcountHelper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
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
  private final CsfFrenchHeadcountHelper csfFrenchHeadcountHelper;
  private final CareerHeadcountHelper careerHeadcountHelper;
  private final RestUtils restUtils;
  private final EllHeadcountHelper ellHeadcountHelper;

  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolId(sdcSchoolCollectionID);
    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionData), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionData));
    if (compare) {
      enrollmentHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }
    enrollmentHeadcountHelper.stripZeroColumns(headcountHeaderList.get(1));
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getFrenchHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    var school = this.restUtils.getSchoolBySchoolID(String.valueOf(sdcSchoolCollectionEntity.getSchoolID()));
    
    List<HeadcountHeader> headcountHeaderList;
    HeadcountResultsTable collectionData;
    if(school.isPresent() && school.get().getSchoolReportingRequirementCode().equals(SchoolReportingRequirementCodes.CSF.getCode())) {
      List<CsfFrenchHeadcountResult> collectionRawData;
      collectionRawData = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySchoolId(sdcSchoolCollectionID);
      headcountHeaderList = csfFrenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
      collectionData = csfFrenchHeadcountHelper.convertHeadcountResults(collectionRawData);
      if(compare) {
        csfFrenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      }
    } else {
      List<FrenchHeadcountResult> collectionRawData;
      collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySchoolId(sdcSchoolCollectionID);
      headcountHeaderList = frenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
      collectionData = frenchHeadcountHelper.convertHeadcountResults(collectionRawData);
      if(compare) {
        frenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      }
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

  public SdcSchoolCollectionStudentHeadcounts getEllHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<EllHeadcountResult> collectionRawData =
      sdcSchoolCollectionStudentRepository.getEllHeadcountsBySchoolId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = ellHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcSchoolCollectionID);
    if (compare) {
      ellHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }
}
