package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper=false)
public class BaseSdcSchoolCollection extends BaseRequest {

  private String sdcSchoolCollectionID;

  @NotNull(message = "collectionID cannot be null")
  private String collectionID;

  @NotNull(message = "schoolID cannot be null")
  private String schoolID;

  private String sdcDistrictCollectionID;

  private String uploadDate;

  @Size(max = 255)
  private String uploadFileName;

  @Size(max = 8)
  private String uploadReportDate;

  @Size(max = 10)
  @NotNull(message = "sdcSchoolCollectionStatusCode cannot be null")
  private String sdcSchoolCollectionStatusCode;

  @Size(max = 10)
  private String collectionTypeCode;

  private String collectionOpenDate;

  private String collectionCloseDate;

  private List<SdcSchoolCollectionStudent> students;

}
