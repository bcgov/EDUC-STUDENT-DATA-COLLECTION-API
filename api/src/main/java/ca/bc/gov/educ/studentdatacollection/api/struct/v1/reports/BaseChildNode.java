package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class BaseChildNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  public BaseChildNode(String typeOfProgram, String isHeading, String sequence, boolean isDoubleRow) {
    this.typeOfProgram = typeOfProgram;
    this.isHeading = isHeading;
    this.sequence = sequence;
    this.isDoubleRow = isDoubleRow;
    if(isDoubleRow){
      setStringValuesForAll("0.0000");
    }
  }

  private static final String DOUBLE_FORMAT = "%,.4f";

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

  private boolean isDoubleRow = false;

  private String sequence;

  private void setStringValuesForAll(String value){
    valueGradeKF = value;
    valueGrade01 = value;
    valueGrade02 = value;
    valueGrade03 = value;
    valueGrade04 = value;
    valueGrade05 = value;
    valueGrade06 = value;
    valueGrade07 = value;
    valueGradeEU = value;
    valueGrade08 = value;
    valueGrade09 = value;
    valueGrade10 = value;
    valueGrade11 = value;
    valueGrade12 = value;
    valueGradeSU = value;
    valueTotal = value;
  }

  public String getValueTotal() {
    if(valueTotal == null){
      return null;
    }
    if(!isDoubleRow) {
      int total = 0;
      total = addIntValueIfPresent(total, valueGradeKF);
      total = addIntValueIfPresent(total, valueGrade01);
      total = addIntValueIfPresent(total, valueGrade02);
      total = addIntValueIfPresent(total, valueGrade03);
      total = addIntValueIfPresent(total, valueGrade04);
      total = addIntValueIfPresent(total, valueGrade05);
      total = addIntValueIfPresent(total, valueGrade06);
      total = addIntValueIfPresent(total, valueGrade07);
      total = addIntValueIfPresent(total, valueGradeEU);
      total = addIntValueIfPresent(total, valueGrade08);
      total = addIntValueIfPresent(total, valueGrade09);
      total = addIntValueIfPresent(total, valueGrade10);
      total = addIntValueIfPresent(total, valueGrade11);
      total = addIntValueIfPresent(total, valueGrade12);
      total = addIntValueIfPresent(total, valueGradeSU);
      valueTotal = Integer.toString(total);
    }else{
      double total = 0;
      total = addDoubleValueIfPresent(total, valueGradeKF);
      total = addDoubleValueIfPresent(total, valueGrade01);
      total = addDoubleValueIfPresent(total, valueGrade02);
      total = addDoubleValueIfPresent(total, valueGrade03);
      total = addDoubleValueIfPresent(total, valueGrade04);
      total = addDoubleValueIfPresent(total, valueGrade05);
      total = addDoubleValueIfPresent(total, valueGrade06);
      total = addDoubleValueIfPresent(total, valueGrade07);
      total = addDoubleValueIfPresent(total, valueGradeEU);
      total = addDoubleValueIfPresent(total, valueGrade08);
      total = addDoubleValueIfPresent(total, valueGrade09);
      total = addDoubleValueIfPresent(total, valueGrade10);
      total = addDoubleValueIfPresent(total, valueGrade11);
      total = addDoubleValueIfPresent(total, valueGrade12);
      total = addDoubleValueIfPresent(total, valueGradeSU);
      valueTotal = String.format(DOUBLE_FORMAT, Double.valueOf(total));
    }

    return valueTotal;
  }

  private int addIntValueIfPresent(int total, String value){
    if(StringUtils.isNotEmpty(value)) {
      total += Integer.valueOf(value);
    }
    return total;
  }

  private double addDoubleValueIfPresent(double total, String value){
    if(StringUtils.isNotEmpty(value)) {
      total += Double.valueOf(value);
    }
    return total;
  }

  public void setAllValuesToNull(){
    setStringValuesForAll(null);
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
