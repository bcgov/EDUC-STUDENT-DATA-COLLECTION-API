package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;
import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper=false)
public class SdcDuplicatesByInstituteID extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  private List<SdcDuplicate> sdcDuplicates;
  private int numProgramDuplicates;
  private int numEnrollmentDuplicates;
}
