package ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SimpleHeadcountResultsTable {
  List<String> headers;
  List<Map<String, String>> rows;
}
