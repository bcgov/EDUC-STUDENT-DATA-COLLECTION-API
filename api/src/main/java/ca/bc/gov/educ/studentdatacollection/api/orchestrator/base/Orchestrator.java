package ca.bc.gov.educ.studentdatacollection.api.orchestrator.base;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSaga;
import org.springframework.data.util.Pair;

import java.io.IOException;
import java.util.List;
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
  void startSaga(SdcSaga saga);

  /**
   * create saga.
   *
   * @param payload   the payload
   * @param sdcSchoolStudentID the student id
   * @param userName  the user who created the saga
   * @return the saga
   */
  SdcSaga createSaga(String payload, UUID sdcSchoolStudentID, String userName);

  /**
   * create multiple sagas.
   *
   * @param payloads   the list of  pair of student id and payload
   * @param userName  the user who created the
   * @param processingYear the processing year
   * @return the saga
   */
  List<SdcSaga> createMultipleSagas(List<Pair<UUID, String>> payloads, String userName, String processingYear);

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(SdcSaga saga) throws IOException, InterruptedException, TimeoutException;
}
