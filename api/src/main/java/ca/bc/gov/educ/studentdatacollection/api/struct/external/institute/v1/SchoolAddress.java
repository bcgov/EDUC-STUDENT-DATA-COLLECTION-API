package ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseAddress;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * The type Student.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SchoolAddress extends BaseAddress implements Serializable {

  private static final long serialVersionUID = 1L;

  private String schoolAddressId;

  private String schoolId;

}
