package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class SoftDeleteRecordSet implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  private List<UUID> softDeleteStudentIDs;

  private String updateUser;
}
