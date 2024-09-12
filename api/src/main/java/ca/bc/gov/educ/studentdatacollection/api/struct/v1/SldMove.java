package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SldMove {
  String toStudentPen;
  List<UUID> sdcSchoolCollectionIdsToUpdate;
}
