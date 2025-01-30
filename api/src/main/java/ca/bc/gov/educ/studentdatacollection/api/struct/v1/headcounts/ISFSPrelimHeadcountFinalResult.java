package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ISFSPrelimHeadcountFinalResult {
  String specialEducationLevel1Count;
  String specialEducationLevel2Count;
  String specialEducationLevel3Count;
  String specialEducationLevelOtherCount;

  String standardAdultsKto3Fte;
  String standardAdults4to7EUFte;
  String standardAdults8to10SUFte;
  String standardAdults11and12Fte;

  String dLAdultsKto9Fte;
  String dLAdults10to12Fte;

  String standardSchoolAgedKHFte;
  String standardSchoolAgedKFFte;
  String standardSchoolAged1to3Fte;
  String standardSchoolAged4to7EUFte;
  String standardSchoolAged8to10SUFte;
  String standardSchoolAged11and12Fte;

  String dLSchoolAgedKto9Fte;
  String dLSchoolAged10to12Fte;

  String totalHomeschoolCount;
}
