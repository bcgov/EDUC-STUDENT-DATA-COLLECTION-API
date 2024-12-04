package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.CollectionStatus;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.IndependentSchoolFundingGroup;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolGrade;
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

  public static String getFundingGroupForGrade(School school, String gradeCode) {
    return school.getSchoolFundingGroups()
            .stream()
            .filter(group -> gradeCode.equals(group.getSchoolGradeCode()))
            .map(IndependentSchoolFundingGroup::getSchoolFundingGroupCode)
            .findFirst()
            .orElse(null);
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

  public static String getProjectedGrade(SdcSchoolCollectionStudentEntity student) {
    if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE03.getCode())) {
      return SchoolGradeCodes.GRADE04.getCode();
    } else if(student.getEnrolledGradeCode().equalsIgnoreCase(SchoolGradeCodes.GRADE06.getCode())) {
      return SchoolGradeCodes.GRADE07.getCode();
    }
    return null;
  }

  public static int addValueIfExists(int totalValue, String actualValue){
    int value;
    try{
      value = Integer.parseInt(actualValue);
    } catch (Exception e) {
      value = 0;
    }
    return totalValue + value;
  }

  public static String getNetChange(String septValue, String febValue){
    if(StringUtils.isNumeric(septValue) && StringUtils.isNumeric(febValue)){
      var change = Integer.parseInt(febValue) - Integer.parseInt(septValue);
      return Integer.toString(change);
    }
    return "0";
  }

  public static String getPositiveChange(String septValue, String febValue){
    if(StringUtils.isNumeric(septValue) && StringUtils.isNumeric(febValue)){
      var change = Integer.parseInt(febValue) - Integer.parseInt(septValue);
      if(change > 0){
        return Integer.toString(change);
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
