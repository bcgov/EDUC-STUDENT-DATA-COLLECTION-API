package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class BaseSDCSchool extends BaseRequest {

  private String sdcSchoolID;

  @NotNull(message = "collectionID cannot be null")
  private String collectionID;

  @NotNull(message = "schoolID cannot be null")
  private String schoolID;

  private String uploadDate;

  @Size(max = 255)
  private String uploadFileName;

  @Size(max = 10)
  @NotNull(message = "collectionStatusTypeCode cannot be null")
  private String collectionStatusTypeCode;

}
