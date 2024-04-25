package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class SdcDuplicate extends BaseRequest implements Serializable {

  private static final long serialVersionUID = 1L;

  @NotNull(message = "sdcDuplicateID cannot be null")
  private String sdcDuplicateID;

  private String sdcDistrictCollectionID;

  SdcSchoolCollectionStudent sdcSchoolCollectionStudent1Entity;

  SdcSchoolCollectionStudent sdcSchoolCollectionStudent2Entity;

  SdcSchoolCollectionStudent retainedSdcSchoolCollectionStudentEntity;

  private String duplicateSeverityCode;

  private String duplicateTypeCode;

  private String programDuplicateTypeCode;

}
