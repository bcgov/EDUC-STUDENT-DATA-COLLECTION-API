package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@NoArgsConstructor
@SuperBuilder
@SuppressWarnings("squid:S1700")
public class HeadcountChildNode implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  public HeadcountChildNode(String typeOfProgram, String isHeading, String sequence) {
    this.typeOfProgram = typeOfProgram;
    this.isHeading = isHeading;
    this.sequence = sequence;
  }

  private String typeOfProgram;

  private String isHeading;

  private String sequence;
}
