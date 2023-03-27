package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import lombok.*;



@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SdcFileUpload {
  @NotNull
  String sdcSchoolCollectionID;
  @NotNull
  String fileName;
  @NotNull
  String createUser;
  @NotNull
  @ToString.Exclude
  String fileContents;
}
