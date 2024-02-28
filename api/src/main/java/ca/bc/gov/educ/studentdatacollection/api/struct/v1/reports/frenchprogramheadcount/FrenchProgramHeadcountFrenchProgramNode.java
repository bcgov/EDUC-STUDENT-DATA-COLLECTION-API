package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.frenchprogramheadcount;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class FrenchProgramHeadcountFrenchProgramNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  public FrenchProgramHeadcountFrenchProgramNode(String typeOfProgram, String isHeading, String sequence) {
    this.typeOfProgram = typeOfProgram;
    this.isHeading = isHeading;
    this.sequence = sequence;
  }

  private String typeOfProgram;

  private String valueGradeKF = "0";

  private String valueGrade01 = "0";

  private String valueGrade02 = "0";

  private String valueGrade03 = "0";

  private String valueGrade04 = "0";

  private String valueGrade05 = "0";

  private String valueGrade06 = "0";

  private String valueGrade07 = "0";

  private String valueGradeEU = "0";

  private String valueGrade08 = "0";

  private String valueGrade09 = "0";

  private String valueGrade10 = "0";

  private String valueGrade11 = "0";

  private String valueGrade12 = "0";

  private String valueGradeSU = "0";

  private String valueTotal = "0";

  private String isHeading;

  private String sequence;

  public String getValueTotal() {
    int total = 0;
    total += Integer.valueOf(valueGradeKF);
    total += Integer.valueOf(valueGrade01);
    total += Integer.valueOf(valueGrade02);
    total += Integer.valueOf(valueGrade03);
    total += Integer.valueOf(valueGrade04);
    total += Integer.valueOf(valueGrade05);
    total += Integer.valueOf(valueGrade06);
    total += Integer.valueOf(valueGrade07);
    total += Integer.valueOf(valueGradeEU);
    total += Integer.valueOf(valueGrade08);
    total += Integer.valueOf(valueGrade09);
    total += Integer.valueOf(valueGrade10);
    total += Integer.valueOf(valueGrade11);
    total += Integer.valueOf(valueGrade12);
    total += Integer.valueOf(valueGradeSU);

    valueTotal = Integer.toString(total);
    return valueTotal;
  }

  public void setValueForGrade(SchoolGradeCodes gradeCode, String value){
    switch (gradeCode){
      case KINDFULL:
        setValueGradeKF(value);
        break;
      case GRADE01:
        setValueGrade01(value);
        break;
      case GRADE02:
        setValueGrade02(value);
        break;
      case GRADE03:
        setValueGrade03(value);
        break;
      case GRADE04:
        setValueGrade04(value);
        break;
      case GRADE05:
        setValueGrade05(value);
        break;
      case GRADE06:
        setValueGrade06(value);
        break;
      case GRADE07:
        setValueGrade07(value);
        break;
      case ELEMUNGR:
        setValueGradeEU(value);
        break;
      case GRADE08:
        setValueGrade08(value);
        break;
      case GRADE09:
        setValueGrade09(value);
        break;
      case GRADE10:
        setValueGrade10(value);
        break;
      case GRADE11:
        setValueGrade11(value);
        break;
      case GRADE12:
        setValueGrade12(value);
        break;
      case SECONDARY_UNGRADED:
        setValueGradeSU(value);
        break;
      default:
        break;
    }
  }

}
