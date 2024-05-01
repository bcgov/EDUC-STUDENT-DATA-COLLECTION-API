package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class SdcDistrictCollection extends BaseRequest {

  private String sdcDistrictCollectionID;

  @NotNull(message = "collectionID cannot be null")
  private String collectionID;

  @NotNull(message = "districtID cannot be null")
  private String districtID;

  @Size(max = 10)
  private String collectionTypeCode;

  @Size(max = 10)
  @NotNull(message = "sdcDistrictCollectionStatusCode cannot be null")
  private String sdcDistrictCollectionStatusCode;

  private String collectionOpenDate;

  private String collectionCloseDate;

  private String submissionDueDate;

}