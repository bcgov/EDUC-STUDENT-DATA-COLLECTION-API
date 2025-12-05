package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolGrade;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySchoolGradeFundingGroupHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySchoolHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndySpecialEdAdultHeadcountResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.beans.Expression;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.capitalize;

/**
 * The type Transform util.
 */

@Slf4j
public class TransformUtil {

  public static final String GROUP_1 = "GROUP1";
  public static final String GROUP_2 = "GROUP2";
  private static final String [] VALID_SCHOOL_FUNDING_GROUPS = new String[]{GROUP_1, GROUP_2};
  public static final String GROUP_3 = "GROUP3";
  public static final String GROUP_4 = "GROUP4";
  public static final String GROUP_1_LEGACY = "01";
  public static final String GROUP_2_LEGACY = "02";
  public static final String GROUP_3_LEGACY = "03";
  public static final String GROUP_4_LEGACY = "04";

  private TransformUtil() {
  }

  /**
   * Uppercase fields t.
   *
   * @param <T>    the type parameter
   * @param claz the claz
   * @return the t
   */
  public static <T> T uppercaseFields(T claz) {
    var clazz = claz.getClass();
    List<Field> fields = new ArrayList<>();
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      fields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
      superClazz = superClazz.getSuperclass();
    }
    fields.forEach(field -> TransformUtil.transformFieldToUppercase(field, claz));
    return claz;
  }

  /**
   * Is uppercase field boolean.
   *
   * @param clazz     the clazz
   * @param fieldName the field name
   * @return the boolean
   */
  public static boolean isUppercaseField(Class<?> clazz, String fieldName) {
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      try {
        Field field = superClazz.getDeclaredField(fieldName);
        return field.getAnnotation(UpperCase.class) != null;
      } catch (NoSuchFieldException e) {
        superClazz = superClazz.getSuperclass();
      }
    }
    return false;
  }

  /**
   * Parses the `NUMBER_OF_COURSES` field, which comes from the database as string that contains an integer that is
   * actually a Double to the hundredth place.
   *
   * @param string - the four didgit float string, eg: "0800"
   * @return a Double, eg: 8.00
   */
  public static Double parseNumberOfCourses(String string, UUID studentId) {
    if (StringUtils.isEmpty(string)) { return 0D; }
    try {
      return Double.parseDouble(string) / 100;
    } catch (NumberFormatException e) {
      log.warn("Could not parse NUMBER_OF_COURSES for collection student ID {}, database value: {}", studentId, string);
      return 0D;
    }
  }

  private static <T> void transformFieldToUppercase(Field field, T claz) {
    if (!field.getType().equals(String.class)) {
      return;
    }

    if (field.getAnnotation(UpperCase.class) != null) {
      try {
        var fieldName = capitalize(field.getName());
        var expr = new Expression(claz, "get" + fieldName, new Object[0]);
        var entityFieldValue = (String) expr.getValue();
        if (entityFieldValue != null) {
          var stmt = new Statement(claz, "set" + fieldName, new Object[]{entityFieldValue.toUpperCase()});
          stmt.execute();
        }
      } catch (Exception ex) {
        throw new StudentDataCollectionAPIRuntimeException(ex.getMessage());
      }
    }
  }

  public static List<String> splitIntoChunks(String text, int numberOfCharacters) {
    String[] results = text.split("(?<=\\G.{" + numberOfCharacters + "})");

    return Arrays.asList(results);
  }

  public static String getGradesOfferedString(School school){
    var gradeList = new ArrayList<SchoolGradeCodes>();
    for(SchoolGrade schoolGrade: school.getGrades()) {
      var optGrade = SchoolGradeCodes.findByTypeCode(schoolGrade.getSchoolGradeCode());
      if(optGrade.isPresent()) {
        gradeList.add(optGrade.get());
      }
    }

    gradeList.sort(Comparator.comparing(SchoolGradeCodes::getSequence));
    StringBuilder gradesOffered = new StringBuilder();
    for(SchoolGradeCodes schoolGrade: gradeList) {
      gradesOffered.append(schoolGrade.getCode() + ",");
    }
    var finalGrades = gradesOffered.toString();
    if(StringUtils.isEmpty(finalGrades)){
      return "";
    }
    return gradesOffered.toString().substring(0, gradesOffered.lastIndexOf(","));
  }

  public static String getTotalHeadcount(IndySchoolHeadcountResult result){
    int total = 0;

    total +=  Integer.parseInt(result.getKindHCount());
    total +=  Integer.parseInt(result.getKindFCount());
    total +=  Integer.parseInt(result.getGrade1Count());
    total +=  Integer.parseInt(result.getGrade2Count());
    total +=  Integer.parseInt(result.getGrade3Count());
    total +=  Integer.parseInt(result.getGrade4Count());
    total +=  Integer.parseInt(result.getGrade5Count());
    total +=  Integer.parseInt(result.getGrade6Count());
    total +=  Integer.parseInt(result.getGrade7Count());
    total +=  Integer.parseInt(result.getGradeEUCount());
    total +=  Integer.parseInt(result.getGrade8Count());
    total +=  Integer.parseInt(result.getGrade9Count());
    total +=  Integer.parseInt(result.getGrade10Count());
    total +=  Integer.parseInt(result.getGrade11Count());
    total +=  Integer.parseInt(result.getGrade12Count());
    total +=  Integer.parseInt(result.getGradeSUCount());
    total +=  Integer.parseInt(result.getGradeGACount());
    total +=  Integer.parseInt(result.getGradeHSCount());

    return Integer.toString(total);
  }

  // Core French (08)
  public static String getTotalHeadcountCF(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountCF());
    total +=  Integer.parseInt(result.getKindFCountCF());
    total +=  Integer.parseInt(result.getGrade1CountCF());
    total +=  Integer.parseInt(result.getGrade2CountCF());
    total +=  Integer.parseInt(result.getGrade3CountCF());
    total +=  Integer.parseInt(result.getGrade4CountCF());
    total +=  Integer.parseInt(result.getGrade5CountCF());
    total +=  Integer.parseInt(result.getGrade6CountCF());
    total +=  Integer.parseInt(result.getGrade7CountCF());
    total +=  Integer.parseInt(result.getGradeEUCountCF());
    total +=  Integer.parseInt(result.getGrade8CountCF());
    total +=  Integer.parseInt(result.getGrade9CountCF());
    total +=  Integer.parseInt(result.getGrade10CountCF());
    total +=  Integer.parseInt(result.getGrade11CountCF());
    total +=  Integer.parseInt(result.getGrade12CountCF());
    total +=  Integer.parseInt(result.getGradeSUCountCF());
    total +=  Integer.parseInt(result.getGradeGACountCF());
    return Integer.toString(total);
  }

  // Programme Francophone (05)
  public static String getTotalHeadcountPF(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountPF());
    total +=  Integer.parseInt(result.getKindFCountPF());
    total +=  Integer.parseInt(result.getGrade1CountPF());
    total +=  Integer.parseInt(result.getGrade2CountPF());
    total +=  Integer.parseInt(result.getGrade3CountPF());
    total +=  Integer.parseInt(result.getGrade4CountPF());
    total +=  Integer.parseInt(result.getGrade5CountPF());
    total +=  Integer.parseInt(result.getGrade6CountPF());
    total +=  Integer.parseInt(result.getGrade7CountPF());
    total +=  Integer.parseInt(result.getGradeEUCountPF());
    total +=  Integer.parseInt(result.getGrade8CountPF());
    total +=  Integer.parseInt(result.getGrade9CountPF());
    total +=  Integer.parseInt(result.getGrade10CountPF());
    total +=  Integer.parseInt(result.getGrade11CountPF());
    total +=  Integer.parseInt(result.getGrade12CountPF());
    total +=  Integer.parseInt(result.getGradeSUCountPF());
    total +=  Integer.parseInt(result.getGradeGACountPF());
    return Integer.toString(total);
  }

  // Early French Immersion (11)
  public static String getTotalHeadcountEFI(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountEFI());
    total +=  Integer.parseInt(result.getKindFCountEFI());
    total +=  Integer.parseInt(result.getGrade1CountEFI());
    total +=  Integer.parseInt(result.getGrade2CountEFI());
    total +=  Integer.parseInt(result.getGrade3CountEFI());
    total +=  Integer.parseInt(result.getGrade4CountEFI());
    total +=  Integer.parseInt(result.getGrade5CountEFI());
    total +=  Integer.parseInt(result.getGrade6CountEFI());
    total +=  Integer.parseInt(result.getGrade7CountEFI());
    total +=  Integer.parseInt(result.getGradeEUCountEFI());
    total +=  Integer.parseInt(result.getGrade8CountEFI());
    total +=  Integer.parseInt(result.getGrade9CountEFI());
    total +=  Integer.parseInt(result.getGrade10CountEFI());
    total +=  Integer.parseInt(result.getGrade11CountEFI());
    total +=  Integer.parseInt(result.getGrade12CountEFI());
    total +=  Integer.parseInt(result.getGradeSUCountEFI());
    total +=  Integer.parseInt(result.getGradeGACountEFI());
    return Integer.toString(total);
  }

  // Late French Immersion (14)
  public static String getTotalHeadcountLFI(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountLFI());
    total +=  Integer.parseInt(result.getKindFCountLFI());
    total +=  Integer.parseInt(result.getGrade1CountLFI());
    total +=  Integer.parseInt(result.getGrade2CountLFI());
    total +=  Integer.parseInt(result.getGrade3CountLFI());
    total +=  Integer.parseInt(result.getGrade4CountLFI());
    total +=  Integer.parseInt(result.getGrade5CountLFI());
    total +=  Integer.parseInt(result.getGrade6CountLFI());
    total +=  Integer.parseInt(result.getGrade7CountLFI());
    total +=  Integer.parseInt(result.getGradeEUCountLFI());
    total +=  Integer.parseInt(result.getGrade8CountLFI());
    total +=  Integer.parseInt(result.getGrade9CountLFI());
    total +=  Integer.parseInt(result.getGrade10CountLFI());
    total +=  Integer.parseInt(result.getGrade11CountLFI());
    total +=  Integer.parseInt(result.getGrade12CountLFI());
    total +=  Integer.parseInt(result.getGradeSUCountLFI());
    total +=  Integer.parseInt(result.getGradeGACountLFI());
    return Integer.toString(total);
  }

  // English Language Learning (17)
  public static String getTotalHeadcountELL(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountELL());
    total +=  Integer.parseInt(result.getKindFCountELL());
    total +=  Integer.parseInt(result.getGrade1CountELL());
    total +=  Integer.parseInt(result.getGrade2CountELL());
    total +=  Integer.parseInt(result.getGrade3CountELL());
    total +=  Integer.parseInt(result.getGrade4CountELL());
    total +=  Integer.parseInt(result.getGrade5CountELL());
    total +=  Integer.parseInt(result.getGrade6CountELL());
    total +=  Integer.parseInt(result.getGrade7CountELL());
    total +=  Integer.parseInt(result.getGradeEUCountELL());
    total +=  Integer.parseInt(result.getGrade8CountELL());
    total +=  Integer.parseInt(result.getGrade9CountELL());
    total +=  Integer.parseInt(result.getGrade10CountELL());
    total +=  Integer.parseInt(result.getGrade11CountELL());
    total +=  Integer.parseInt(result.getGrade12CountELL());
    total +=  Integer.parseInt(result.getGradeSUCountELL());
    total +=  Integer.parseInt(result.getGradeGACountELL());
    return Integer.toString(total);
  }

  // Indigenous Language and Culture (29)
  public static String getTotalHeadcountALC(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountALC());
    total +=  Integer.parseInt(result.getKindFCountALC());
    total +=  Integer.parseInt(result.getGrade1CountALC());
    total +=  Integer.parseInt(result.getGrade2CountALC());
    total +=  Integer.parseInt(result.getGrade3CountALC());
    total +=  Integer.parseInt(result.getGrade4CountALC());
    total +=  Integer.parseInt(result.getGrade5CountALC());
    total +=  Integer.parseInt(result.getGrade6CountALC());
    total +=  Integer.parseInt(result.getGrade7CountALC());
    total +=  Integer.parseInt(result.getGradeEUCountALC());
    total +=  Integer.parseInt(result.getGrade8CountALC());
    total +=  Integer.parseInt(result.getGrade9CountALC());
    total +=  Integer.parseInt(result.getGrade10CountALC());
    total +=  Integer.parseInt(result.getGrade11CountALC());
    total +=  Integer.parseInt(result.getGrade12CountALC());
    total +=  Integer.parseInt(result.getGradeSUCountALC());
    total +=  Integer.parseInt(result.getGradeGACountALC());
    return Integer.toString(total);
  }

  // Indigenous Support Services (33)
  public static String getTotalHeadcountASS(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountASS());
    total +=  Integer.parseInt(result.getKindFCountASS());
    total +=  Integer.parseInt(result.getGrade1CountASS());
    total +=  Integer.parseInt(result.getGrade2CountASS());
    total +=  Integer.parseInt(result.getGrade3CountASS());
    total +=  Integer.parseInt(result.getGrade4CountASS());
    total +=  Integer.parseInt(result.getGrade5CountASS());
    total +=  Integer.parseInt(result.getGrade6CountASS());
    total +=  Integer.parseInt(result.getGrade7CountASS());
    total +=  Integer.parseInt(result.getGradeEUCountASS());
    total +=  Integer.parseInt(result.getGrade8CountASS());
    total +=  Integer.parseInt(result.getGrade9CountASS());
    total +=  Integer.parseInt(result.getGrade10CountASS());
    total +=  Integer.parseInt(result.getGrade11CountASS());
    total +=  Integer.parseInt(result.getGrade12CountASS());
    total +=  Integer.parseInt(result.getGradeSUCountASS());
    total +=  Integer.parseInt(result.getGradeGACountASS());
    return Integer.toString(total);
  }

  // Other Approved Indigenous Programs (36)
  public static String getTotalHeadcountOAAP(IndySchoolGradeFundingGroupHeadcountResult result){
    int total = 0;
    total +=  Integer.parseInt(result.getKindHCountOAAP());
    total +=  Integer.parseInt(result.getKindFCountOAAP());
    total +=  Integer.parseInt(result.getGrade1CountOAAP());
    total +=  Integer.parseInt(result.getGrade2CountOAAP());
    total +=  Integer.parseInt(result.getGrade3CountOAAP());
    total +=  Integer.parseInt(result.getGrade4CountOAAP());
    total +=  Integer.parseInt(result.getGrade5CountOAAP());
    total +=  Integer.parseInt(result.getGrade6CountOAAP());
    total +=  Integer.parseInt(result.getGrade7CountOAAP());
    total +=  Integer.parseInt(result.getGradeEUCountOAAP());
    total +=  Integer.parseInt(result.getGrade8CountOAAP());
    total +=  Integer.parseInt(result.getGrade9CountOAAP());
    total +=  Integer.parseInt(result.getGrade10CountOAAP());
    total +=  Integer.parseInt(result.getGrade11CountOAAP());
    total +=  Integer.parseInt(result.getGrade12CountOAAP());
    total +=  Integer.parseInt(result.getGradeSUCountOAAP());
    total +=  Integer.parseInt(result.getGradeGACountOAAP());
    return Integer.toString(total);
  }

  /**
   * Generic helper method to determine funding group for a given set of grades.
   * Returns the funding group code only if ALL grades in the list have the same funding group, otherwise empty string.
   *
   * @param fundingGroups List of funding groups for the school
   * @param requiredGrades List of grade codes that must all be present
   * @param facilityTypeCode The school's facility type code (optional - can be null)
   * @param requireDistanceLearning If true, only returns funding group for Distance Learning schools
   * @return Funding group code if all conditions met, empty string otherwise
   */
  private static String getFundingGroupForGrades(List<IndependentSchoolFundingGroup> fundingGroups, List<String> requiredGrades, String facilityTypeCode, boolean requireDistanceLearning) {
    // Check facility type if Distance Learning is required
    if (requireDistanceLearning && (facilityTypeCode == null || !facilityTypeCode.equalsIgnoreCase(FacilityTypeCodes.DIST_LEARN.getCode()))) {
      return "";
    }

    if (fundingGroups == null || fundingGroups.isEmpty()) {
      return "";
    }

    // Get all funding groups that match the grade range
    Set<String> matchingGrades = fundingGroups.stream()
            .map(IndependentSchoolFundingGroup::getSchoolGradeCode)
            .filter(requiredGrades::contains)
            .collect(Collectors.toSet());

    // Check if all required grades are present
    if (!matchingGrades.containsAll(requiredGrades)) {
      return "";
    }

    // Get all funding group codes for this range
    Set<String> fundingGroupCodes = fundingGroups.stream()
            .filter(group -> requiredGrades.contains(group.getSchoolGradeCode()))
            .map(IndependentSchoolFundingGroup::getSchoolFundingGroupCode)
            .collect(Collectors.toSet());

    // Return the number after "GROUP" if all grades have the same one
    if (fundingGroupCodes.size() == 1) {
      String groupCode = fundingGroupCodes.iterator().next();
      return groupCode != null && groupCode.startsWith("GROUP") ? groupCode.substring(5) : "";
    }
    return "";
  }

  public static String getFundingGroupForGradesK3(List<IndependentSchoolFundingGroup> fundingGroups) {
    List<String> k3Grades = Arrays.asList("KINDHALF", "KINDFULL", "GRADE01", "GRADE02", "GRADE03", "ELEMUNGR");
    return getFundingGroupForGrades(fundingGroups, k3Grades, null, false);
  }

  public static String getFundingGroupForGrades47(List<IndependentSchoolFundingGroup> fundingGroups) {
    List<String> grades47 = Arrays.asList("GRADE04", "GRADE05", "GRADE06", "GRADE07");
    return getFundingGroupForGrades(fundingGroups, grades47, null, false);
  }

  public static String getFundingGroupForGrades810(List<IndependentSchoolFundingGroup> fundingGroups) {
    List<String> grades810 = Arrays.asList("GRADE08", "GRADE09", "GRADE10");
    return getFundingGroupForGrades(fundingGroups, grades810, null, false);
  }

  public static String getFundingGroupForGrades1112(List<IndependentSchoolFundingGroup> fundingGroups) {
    List<String> grades1112 = Arrays.asList("GRADE11", "GRADE12");
    return getFundingGroupForGrades(fundingGroups, grades1112, null, false);
  }

  public static String getFundingGroupForDLGradesK9EU(List<IndependentSchoolFundingGroup> fundingGroups, String facilityTypeCode) {
    List<String> dlK9EUGrades = Arrays.asList("KINDHALF", "KINDFULL", "GRADE01", "GRADE02", "GRADE03",
            "GRADE04", "GRADE05", "GRADE06", "GRADE07", "ELEMUNGR", "GRADE08", "GRADE09");
    return getFundingGroupForGrades(fundingGroups, dlK9EUGrades, facilityTypeCode, true);
  }

  public static String getFundingGroupForDLGrades1012(List<IndependentSchoolFundingGroup> fundingGroups, String facilityTypeCode) {
    List<String> dlGrades1012 = Arrays.asList("GRADE10", "GRADE11", "GRADE12");
    return getFundingGroupForGrades(fundingGroups, dlGrades1012, facilityTypeCode, true);
  }

  public static String getTotalHeadcount(IndySpecialEdAdultHeadcountResult result){
    int total = 0;

    total +=  Integer.parseInt(result.getSpecialEdACodes());
    total +=  Integer.parseInt(result.getSpecialEdBCodes());
    total +=  Integer.parseInt(result.getSpecialEdCCodes());
    total +=  Integer.parseInt(result.getSpecialEdDCodes());
    total +=  Integer.parseInt(result.getSpecialEdECodes());
    total +=  Integer.parseInt(result.getSpecialEdFCodes());
    total +=  Integer.parseInt(result.getSpecialEdGCodes());
    total +=  Integer.parseInt(result.getSpecialEdHCodes());
    total +=  Integer.parseInt(result.getSpecialEdKCodes());
    total +=  Integer.parseInt(result.getSpecialEdPCodes());
    total +=  Integer.parseInt(result.getSpecialEdQCodes());
    total +=  Integer.parseInt(result.getSpecialEdRCodes());

    return Integer.toString(total);
  }

  public static String flagSpecialEdHeadcountIfRequired(String value, boolean adultValue){
    if(!adultValue){
      return value;
    }
    return value + "*";
  }

  public static String flagCountIfNoSchoolFundingGroup(String schoolGradeCode, List<String> schoolFundingGroupGrades, String value){
    if(schoolFundingGroupGrades.contains(schoolGradeCode) || (StringUtils.isNotEmpty(value) && value.equals("0"))){
      return value;
    }
    return value + "*";
  }

  public static String getFundingGroupForGrade(List<IndependentSchoolFundingGroup> schoolFundingGroups, String gradeCode) {
    return schoolFundingGroups
            .stream()
            .filter(group -> gradeCode.equals(group.getSchoolGradeCode()))
            .map(IndependentSchoolFundingGroup::getSchoolFundingGroupCode)
            .findFirst()
            .orElse(null);
  }

  public static String getFundingGroupSnapshotForGrade(List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups, String gradeCode) {
    return schoolFundingGroups
            .stream()
            .filter(group -> gradeCode.equals(group.getSchoolGradeCode()))
            .map(IndependentSchoolFundingGroupSnapshotEntity::getSchoolFundingGroupCode)
            .findFirst()
            .orElse(null);
  }

  public static String getLowestFundingGroupForGrade(List<IndependentSchoolFundingGroup> schoolFundingGroups, List<String> gradeCodeList) {
    String currentGroupCode = null;
    for(String grade: gradeCodeList){
      var fundingGroup = schoolFundingGroups
              .stream()
              .filter(group -> grade.equals(group.getSchoolGradeCode()))
              .map(IndependentSchoolFundingGroup::getSchoolFundingGroupCode)
              .findFirst()
              .orElse(null);
      currentGroupCode = compareForLowestValue(currentGroupCode, fundingGroup);
    }

    return getLegacyCodeFromGroup(currentGroupCode);
  }

  public static String getLowestFundingGroupSnapshotForGroup(List<IndependentSchoolFundingGroupSnapshotEntity> schoolFundingGroups, List<String> gradeCodeList) {
    String currentGroupCode = null;
    for(String grade: gradeCodeList){
      var fundingGroup = schoolFundingGroups
              .stream()
              .filter(group -> grade.equals(group.getSchoolGradeCode()))
              .map(IndependentSchoolFundingGroupSnapshotEntity::getSchoolFundingGroupCode)
              .findFirst()
              .orElse(null);
      currentGroupCode = compareForLowestValue(currentGroupCode, fundingGroup);
    }

    return getLegacyCodeFromGroup(currentGroupCode);
  }

  private static String getLegacyCodeFromGroup(String groupCode){
    if(StringUtils.isBlank(groupCode)){
      return null;
    }
    switch (groupCode){
      case GROUP_1 -> {
          return GROUP_1_LEGACY;
      }
      case GROUP_2 -> {
        return GROUP_2_LEGACY;
      }
      case GROUP_3 -> {
        return GROUP_3_LEGACY;
      }
      case GROUP_4 -> {
        return GROUP_4_LEGACY;
      }
    }
    return null;
  }

  private static String compareForLowestValue(String currentGroupCode, String groupCode){
    if(StringUtils.isEmpty(groupCode)){
      return currentGroupCode;
    }else if(StringUtils.isEmpty(currentGroupCode) && StringUtils.isNotBlank(groupCode)){
      return groupCode;
    }else if(StringUtils.isEmpty(currentGroupCode) && StringUtils.isEmpty(groupCode)){
      return currentGroupCode;
    }

    if(currentGroupCode.equals(GROUP_1) || groupCode.equals(GROUP_1)){
      return GROUP_1;
    }else if(currentGroupCode.equals(GROUP_2) || groupCode.equals(GROUP_2)){
      return GROUP_2;
    }else if(currentGroupCode.equals(GROUP_3) || groupCode.equals(GROUP_3)){
      return GROUP_3;
    }else if(currentGroupCode.equals(GROUP_4) || groupCode.equals(GROUP_4)){
      return GROUP_4;
    }
    return null;
  }

  public static boolean isSchoolFundingGroup1orGroup2(String schoolFundingGroup) {
    if(schoolFundingGroup == null){
      return false;
    }
    return Arrays.stream(VALID_SCHOOL_FUNDING_GROUPS).anyMatch(group -> group.equals(schoolFundingGroup));
  }

  public static String sanitizeEnrolledProgramString(String enrolledProgramCode) {
    if(StringUtils.isEmpty(enrolledProgramCode) || Pattern.compile("^[0\\s]*$").matcher(enrolledProgramCode).matches()) {
      return null;
    }

    if(!StringUtils.isNumeric(enrolledProgramCode.stripTrailing()) || enrolledProgramCode.stripTrailing().length() % 2 != 0 || enrolledProgramCode.stripTrailing().contains(" ")) {
      return enrolledProgramCode.stripTrailing();
    } else {
      return splitIntoChunks(enrolledProgramCode.stripTrailing(), 2).stream().filter(codes -> !codes.equals("00")).collect(Collectors.joining());
    }
  }

  public static void clearCalculatedFields(SdcSchoolCollectionStudentEntity incomingStudentEntity, boolean wipePENMatch){
    if(wipePENMatch) {
      incomingStudentEntity.setAssignedStudentId(null);
      incomingStudentEntity.setAssignedPen(null);
      incomingStudentEntity.setPenMatchResult(null);
    }

    incomingStudentEntity.setFte(null);
    incomingStudentEntity.setIsGraduated(null);
    incomingStudentEntity.setIsSchoolAged(null);
    incomingStudentEntity.setIsAdult(null);
    incomingStudentEntity.setYearsInEll(null);
    incomingStudentEntity.setNumberOfCoursesDec(null);
    incomingStudentEntity.setFteZeroReasonCode(null);
    incomingStudentEntity.setCareerProgramNonEligReasonCode(null);
    incomingStudentEntity.setSpecialEducationNonEligReasonCode(null);
    incomingStudentEntity.setEllNonEligReasonCode(null);
    incomingStudentEntity.setIndigenousSupportProgramNonEligReasonCode(null);
    incomingStudentEntity.setFrenchProgramNonEligReasonCode(null);
  }

  public static String getProjectedGrade(SdcSchoolCollectionStudentFsaReportEntity student) {
    if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE03.getCode())) {
      return SchoolGradeCodes.GRADE04.getCode();
    } else if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE06.getCode())) {
      return SchoolGradeCodes.GRADE07.getCode();
    }
    return null;
  }

  public static double addValueIfExists(double currentTotal, String valueToAddStr) {
    if (StringUtils.isNotBlank(valueToAddStr)) {
      try {
        double valueToAdd = Double.parseDouble(valueToAddStr);
        return currentTotal + valueToAdd;
      } catch (NumberFormatException e) {
        return currentTotal;
      }
    }
    return currentTotal;
  }

  public static String getNetChange(String septValueStr, String febValueStr) {
    Double septValue = null;
    Double febValue = null;

    if (StringUtils.isNotBlank(septValueStr)) {
      try { septValue = Double.parseDouble(septValueStr); }
      catch (NumberFormatException e) { septValue = 0.0; }
    }
    if (StringUtils.isNotBlank(febValueStr)) {
      try { febValue = Double.parseDouble(febValueStr); }
      catch (NumberFormatException e) { febValue = 0.0; }
    }

    if (septValue != null && febValue != null) {
      double change = febValue - septValue;
      String changeStr = Double.toString(change);
      if (changeStr.endsWith(".0")) {
        return changeStr.substring(0, changeStr.length() - 2);
      }
      return changeStr;
    }
    return "0";
  }

  public static String getPositiveChange(String septValueStr, String febValueStr) {
    Double septValue = null;
    Double febValue = null;

    if (StringUtils.isNotBlank(septValueStr)) {
      try {
        septValue = Double.parseDouble(septValueStr);
      } catch (NumberFormatException e) {
        septValue = 0.0;
      }
    }

    if (StringUtils.isNotBlank(febValueStr)) {
      try {
        febValue = Double.parseDouble(febValueStr);
      } catch (NumberFormatException e) {
        febValue = 0.0;
      }
    }

    if (septValue != null && febValue != null) {
      double change = febValue - septValue;
      if (change > 0) {
        String changeStr = Double.toString(change);
        if (changeStr.endsWith(".0")) {
          return changeStr.substring(0, changeStr.length() - 2);
        }
        return changeStr;
      }
    }
    return "0";
  }

  public static String getFTEPositiveChange(String septValue, String febValue){
    if(NumberUtils.isCreatable(septValue) && NumberUtils.isCreatable(febValue)){
      var change = Double.parseDouble(febValue) - Double.parseDouble(septValue);
      if(change > 0){
        return String.format("%.4f", change);
      }
    }
    return "0.0";
  }

  public static boolean isCollectionInProvDupes(CollectionEntity collection){
    return collection.getCollectionStatusCode().equals(CollectionStatus.PROVDUPES.getCode());
  }

  public static void writeEnrolledProgramCodes(SdcSchoolCollectionStudentEntity sdcSchoolCollectionStudentEntity, List<String> enrolledProgramCodes) {
    enrolledProgramCodes.forEach(enrolledProgramCode -> {
      var enrolledProgramEntity = new SdcSchoolCollectionStudentEnrolledProgramEntity();
      enrolledProgramEntity.setSdcSchoolCollectionStudentEntity(sdcSchoolCollectionStudentEntity);
      enrolledProgramEntity.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setUpdateDate(LocalDateTime.now());
      enrolledProgramEntity.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
      enrolledProgramEntity.setCreateDate(LocalDateTime.now());
      enrolledProgramEntity.setEnrolledProgramCode(enrolledProgramCode);
      sdcSchoolCollectionStudentEntity.getSdcStudentEnrolledProgramEntities().add(enrolledProgramEntity);
    });
  }
}
