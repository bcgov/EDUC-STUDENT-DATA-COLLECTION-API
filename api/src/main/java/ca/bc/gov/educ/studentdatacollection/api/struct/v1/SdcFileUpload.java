package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import lombok.*;



@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SdcFileUpload {
  @NotNull
  String schoolID;
  @NotNull
  String createUser;
  @NotNull
  String updateUser;
  @NotNull
  @ToString.Exclude
  String fileContents;
}
