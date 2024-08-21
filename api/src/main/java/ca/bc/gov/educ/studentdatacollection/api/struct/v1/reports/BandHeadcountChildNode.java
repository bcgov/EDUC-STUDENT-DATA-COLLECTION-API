package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class BandHeadcountChildNode extends HeadcountChildNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private String valueFTE = "0";

  private String valueHeadcount = "0";

  public BandHeadcountChildNode(String typeOfProgram, String isHeading, String sequence) {
    super(typeOfProgram, isHeading, sequence);
  }

  public void setValueForBand(String type, String value){
    switch (type){
      case "FTE":
        setValueFTE(value);
        break;
      case "Headcount":
        setValueHeadcount(value);
        break;
      default:
        break;
    }
  }
}
