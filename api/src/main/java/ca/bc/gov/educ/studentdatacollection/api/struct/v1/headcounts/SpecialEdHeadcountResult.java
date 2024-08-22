package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

public interface SpecialEdHeadcountResult extends HeadcountResult {
  String getLevelOnes();
  String getSpecialEdACodes();
  String getSpecialEdBCodes();
  String getLevelTwos();
  String getSpecialEdCCodes();
  String getSpecialEdDCodes();
  String getSpecialEdECodes();
  String getSpecialEdFCodes();
  String getSpecialEdGCodes();
  String getLevelThrees();
  String getSpecialEdHCodes();
  String getOtherLevels();
  String getSpecialEdKCodes();
  String getSpecialEdPCodes();
  String getSpecialEdQCodes();
  String getSpecialEdRCodes();
  String getAllLevels();
  boolean getAdultsInSpecialEdA();
  boolean getAdultsInSpecialEdB();
  boolean getAdultsInSpecialEdC();
  boolean getAdultsInSpecialEdD();
  boolean getAdultsInSpecialEdE();
  boolean getAdultsInSpecialEdF();
  boolean getAdultsInSpecialEdG();
  boolean getAdultsInSpecialEdH();
  boolean getAdultsInSpecialEdK();
  boolean getAdultsInSpecialEdP();
  boolean getAdultsInSpecialEdQ();
  boolean getAdultsInSpecialEdR();
}
