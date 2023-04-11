package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class SdcFileSummaryCounts {
  String studentsWithErrors;

  String schoolAged;
  String adult;
  String total;

  String indigenousStudentsTotal;
  String indigenousLanguage;
  String indigenousSupportServicesProgram;
  String otherIndigenousPrograms;

  String englishLanguageLearner;
  String frenchLanguageLearner;

  String physicallyDependent;
  String deafBlind;
  String moderateProfoundDisability;
  String physicalDisability;
  String visualImpairment;
  String deafHardOfHearing;
  String autismSpectrum;
  String intensiveBehaviour;
  String mildIntellectualDisability;
  String gifted;
  String learningDisability;
  String moderateBehaviorSupport;

  String outOfProvince;
  String livingOnReserve;
  String newcomerRefugee;

  String careerPreparation;
  String cooperativeEducation;
  String youthWorkInTrades;
  String youthTrainInTrades;
  String coreFrench;
  String programmeFrancophone;
  String earlyFrenchImmersion;
  String lateFrenchImmersion;

}
