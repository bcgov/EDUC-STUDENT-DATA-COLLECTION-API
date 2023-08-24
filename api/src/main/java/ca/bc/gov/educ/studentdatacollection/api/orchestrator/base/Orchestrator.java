package ca.bc.gov.educ.studentdatacollection.api.orchestrator.base;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSagaEntity;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * The interface Orchestrator.
 */
public interface Orchestrator {


  /**
   * Gets saga name.
   *
   * @return the saga name
   */
  String getSagaName();

  /**
   * Start saga.
   *
   * @param saga  the saga data
   */
  void startSaga(SdcSagaEntity saga);

  /**
   * create saga.
   *
   * @param payload   the payload
   * @param sdcSchoolStudentID the student id
   * @param userName  the user who created the saga
   * @return the saga
   */
  SdcSagaEntity createSaga(String payload, UUID sdcSchoolStudentID, UUID sdcSchoolCollectionID, String userName);

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(SdcSagaEntity saga) throws IOException, InterruptedException, TimeoutException;
}
