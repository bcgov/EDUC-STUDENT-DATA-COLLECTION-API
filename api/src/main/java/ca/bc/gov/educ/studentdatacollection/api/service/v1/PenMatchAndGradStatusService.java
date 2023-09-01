package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class PenMatchAndGradStatusService {

  private final RestUtils restUtils;

  public PenMatchAndGradStatusService(RestUtils restUtils) {
    this.restUtils = restUtils;
  }

  public void updatePenMatchAndGradStatusColumns(SdcSchoolCollectionStudentEntity student, String mincode) throws EntityNotFoundException {
    var penMatchResult = this.restUtils.getPenMatchResult(UUID.randomUUID(), student, mincode);
    val penMatchResultCode = penMatchResult.getPenStatus();
    student.setPenMatchResult(penMatchResultCode);
    var validPenMatchResults = Arrays.asList("AA", "B1", "C1", "D1");

    if (StringUtils.isNotEmpty(penMatchResultCode) && validPenMatchResults.contains(penMatchResultCode)) {
      final var penMatchRecordOptional = penMatchResult.getMatchingRecords().stream().findFirst();
      if (penMatchRecordOptional.isPresent()) {
        var assignedPEN = penMatchRecordOptional.get().getMatchingPEN();
        var assignedStudentID = penMatchRecordOptional.get().getStudentID();

        student.setAssignedStudentId(UUID.fromString(assignedStudentID));
        student.setAssignedPen(assignedPEN);
      } else {
        log.error("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
        throw new StudentDataCollectionAPIRuntimeException("PenMatchRecord in priority queue is empty for matched status, this should not have happened.");
      }
    }else{
      student.setPenMatchResult("NEW");
    }

    //TODO Change me
    if(student.getIsGraduated() == null) {
      student.setIsGraduated(false);
    }
  }

}
