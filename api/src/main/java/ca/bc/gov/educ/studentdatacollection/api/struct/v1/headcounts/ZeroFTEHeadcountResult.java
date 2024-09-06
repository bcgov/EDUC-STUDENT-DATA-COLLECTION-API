package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

/**
 * This interface is used as data mapper for Non-Funded Summary report.
 */
public interface ZeroFTEHeadcountResult extends HeadcountResult {
    String getFteZeroReasonCode();

    String getGradeKF();

    String getGrade01();

    String getGrade02();

    String getGrade03();

    String getGrade04();

    String getGrade05();

    String getGrade06();

    String getGrade07();

    String getGradeEU();

    String getGrade08();

    String getGrade09();

    String getGrade10();

    String getGrade11();

    String getGrade12();

    String getGradeSU();

    String getGradeGA();

    String getGradeHS();

    String getAllLevels();
}
