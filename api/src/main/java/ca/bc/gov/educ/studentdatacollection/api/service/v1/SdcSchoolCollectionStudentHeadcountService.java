package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolReportingRequirementCodes;
import ca.bc.gov.educ.studentdatacollection.api.helpers.*;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudentHeadcounts;
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
  private final EllHeadcountHelper ellHeadcountHelper;
  private final IndigenousHeadcountHelper indigenousHeadcountHelper;
  private final SpecialEdHeadcountHelper specialEdHeadcountHelper;
  private final BandResidenceHeadcountHelper bandResidenceHeadcountHelper;
  private final RestUtils restUtils;

  public SdcSchoolCollectionStudentHeadcounts getEnrollmentHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    Optional<School> school = this.restUtils.getSchoolBySchoolID(String.valueOf(sdcSchoolCollectionEntity.getSchoolID()));
    enrollmentHeadcountHelper.setGradeCodes(school);
    List<EnrollmentHeadcountResult> collectionRawData = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    HeadcountResultsTable collectionData = enrollmentHeadcountHelper.convertHeadcountResults(collectionRawData);

    List<HeadcountHeader> headcountHeaderList = Arrays.asList(enrollmentHeadcountHelper.getStudentsHeadcountTotals(collectionData), enrollmentHeadcountHelper.getGradesHeadcountTotals(collectionData));
    if (compare) {
      enrollmentHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList, collectionData);
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
      csfFrenchHeadcountHelper.setGradeCodes(school);
      collectionRawData = sdcSchoolCollectionStudentRepository.getCsfFrenchHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
      headcountHeaderList = csfFrenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
      collectionData = csfFrenchHeadcountHelper.convertHeadcountResults(collectionRawData);
      if(compare) {
        csfFrenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
        csfFrenchHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, collectionData);
      }
    } else {
      List<FrenchHeadcountResult> collectionRawData;
      frenchHeadcountHelper.setGradeCodes(school);
      collectionRawData = sdcSchoolCollectionStudentRepository.getFrenchHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
      headcountHeaderList = frenchHeadcountHelper.getHeaders(sdcSchoolCollectionID);
      collectionData = frenchHeadcountHelper.convertHeadcountResults(collectionRawData);
      if(compare) {
        frenchHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
        frenchHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, collectionData);
      }
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(collectionData).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getCareerHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();

    List<CareerHeadcountResult> result = sdcSchoolCollectionStudentRepository.getCareerHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = careerHeadcountHelper.convertHeadcountResults(result);
    List<HeadcountHeader> headcountHeaderList = careerHeadcountHelper.getHeaders(sdcSchoolCollectionID, false);
    if(compare) {
      careerHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      careerHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, headcountResultsTable);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getIndigenousHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    Optional<School> school = this.restUtils.getSchoolBySchoolID(String.valueOf(sdcSchoolCollectionEntity.getSchoolID()));
    indigenousHeadcountHelper.setGradeCodes(school);
    List<IndigenousHeadcountResult> result = sdcSchoolCollectionStudentRepository.getIndigenousHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = indigenousHeadcountHelper.convertHeadcountResults(result);
    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcSchoolCollectionID, false);
    if(compare) {
      indigenousHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      indigenousHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, headcountResultsTable);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getEllHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    Optional<School> school = this.restUtils.getSchoolBySchoolID(String.valueOf(sdcSchoolCollectionEntity.getSchoolID()));
    ellHeadcountHelper.setGradeCodes(school);
    ellHeadcountHelper.setSchoolReportTitles();

    List<EllHeadcountResult> collectionRawData =
      sdcSchoolCollectionStudentRepository.getEllHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = ellHeadcountHelper.convertHeadcountResults(collectionRawData);
    List<HeadcountHeader> headcountHeaderList = ellHeadcountHelper.getHeaders(sdcSchoolCollectionID, false);
    if (compare) {
      ellHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      ellHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, headcountResultsTable);
    }

    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getSpecialEdHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    var sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    Optional<School> school = this.restUtils.getSchoolBySchoolID(String.valueOf(sdcSchoolCollectionEntity.getSchoolID()));
    specialEdHeadcountHelper.setGradeCodes(school);

    List<SpecialEdHeadcountResult> result = sdcSchoolCollectionStudentRepository.getSpecialEdHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    HeadcountResultsTable headcountResultsTable = specialEdHeadcountHelper.convertHeadcountResults(result);
    List<HeadcountHeader> headcountHeaderList = specialEdHeadcountHelper.getHeaders(sdcSchoolCollectionID, false);
    if(compare) {
      specialEdHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      specialEdHeadcountHelper.setResultsTableComparisonValues(sdcSchoolCollectionEntity, headcountResultsTable);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }

  public SdcSchoolCollectionStudentHeadcounts getBandResidenceHeadcounts(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, boolean compare) {
    UUID sdcSchoolCollectionID = sdcSchoolCollectionEntity.getSdcSchoolCollectionID();
    List<BandResidenceHeadcountResult> result = sdcSchoolCollectionStudentRepository.getBandResidenceHeadcountsBySdcSchoolCollectionId(sdcSchoolCollectionID);
    bandResidenceHeadcountHelper.setBandTitles(result);

    List<HeadcountHeader> headcountHeaderList = indigenousHeadcountHelper.getHeaders(sdcSchoolCollectionID, false);
    HeadcountResultsTable headcountResultsTable = bandResidenceHeadcountHelper.convertBandHeadcountResults(result);
    if(compare) {
      indigenousHeadcountHelper.setComparisonValues(sdcSchoolCollectionEntity, headcountHeaderList);
      bandResidenceHeadcountHelper.setBandResultsTableComparisonValues(sdcSchoolCollectionEntity, headcountResultsTable);
    }
    return SdcSchoolCollectionStudentHeadcounts.builder().headcountHeaders(headcountHeaderList).headcountResultsTable(headcountResultsTable).build();
  }
}
