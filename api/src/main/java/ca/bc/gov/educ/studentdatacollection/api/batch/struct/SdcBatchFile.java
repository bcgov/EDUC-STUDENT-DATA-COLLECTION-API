package ca.bc.gov.educ.studentdatacollection.api.batch.struct;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * The type Batch file.
 *
 * @author OM The type Batch file.
 */
@Data
public class SdcBatchFile {
  /**
   * The Batch file header.
   */
  private SdcBatchFileHeader batchFileHeader;
  /**
   * The Student details.
   */
  private List<SdcStudentDetails> studentDetails;
  /**
   * The Batch file trailer.
   */
  private SdcBatchFileTrailer batchFileTrailer;


  /**
   * Gets student details.
   *
   * @return the student details
   */
  public List<SdcStudentDetails> getStudentDetails() {
    if(this.studentDetails == null){
      this.studentDetails = new ArrayList<>();
    }
    return this.studentDetails;
  }
}
