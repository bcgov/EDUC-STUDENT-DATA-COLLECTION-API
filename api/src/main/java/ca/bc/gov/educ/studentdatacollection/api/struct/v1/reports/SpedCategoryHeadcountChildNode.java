package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
public class SpedCategoryHeadcountChildNode extends HeadcountChildNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String valueSpecialEdA = "0";

  private String valueSpecialEdB = "0";

  private String valueSpecialEdC = "0";

  private String valueSpecialEdD = "0";

  private String valueSpecialEdE = "0";

  private String valueSpecialEdF = "0";

  private String valueSpecialEdG = "0";

  private String valueSpecialEdH = "0";

  private String valueSpecialEdK = "0";

  private String valueSpecialEdP = "0";

  private String valueSpecialEdQ = "0";

  private String valueSpecialEdR = "0";

  private String valueSpecialEdTotal = "0";

  public SpedCategoryHeadcountChildNode(String typeOfProgram, String isHeading, String sequence) {
    super(typeOfProgram, isHeading, sequence);
  }

  public void setValueForSpecEdCategory(String category, String value){
    switch (category) {
      case "A" -> setValueSpecialEdA(value);
      case "B" -> setValueSpecialEdB(value);
      case "C" -> setValueSpecialEdC(value);
      case "D" -> setValueSpecialEdD(value);
      case "E" -> setValueSpecialEdE(value);
      case "F" -> setValueSpecialEdF(value);
      case "G" -> setValueSpecialEdG(value);
      case "H" -> setValueSpecialEdH(value);
      case "K" -> setValueSpecialEdK(value);
      case "P" -> setValueSpecialEdP(value);
      case "Q" -> setValueSpecialEdQ(value);
      case "total" -> setValueSpecialEdTotal(value);
      default -> {}
    }
  }
}