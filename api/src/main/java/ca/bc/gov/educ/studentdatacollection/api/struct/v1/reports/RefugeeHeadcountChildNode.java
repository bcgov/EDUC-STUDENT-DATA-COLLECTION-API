package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class RefugeeHeadcountChildNode extends HeadcountChildNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String valueFTE = "0";
  private String valueHeadcount = "0";
  private String valueELL = "0";

  public RefugeeHeadcountChildNode(String typeOfProgram, String isHeading, String sequence) {
    super(typeOfProgram, isHeading, sequence);
  }

  public void setValueForRefugee(String type, String value){
    switch (type){
      case "FTE":
        setValueFTE(value);
        break;
      case "Headcount":
        setValueHeadcount(value);
        break;
      case "ELL":
        setValueELL(value);
        break;
      default:
        break;
    }
  }
}
