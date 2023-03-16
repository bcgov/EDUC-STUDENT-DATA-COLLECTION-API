package ca.bc.gov.educ.studentdatacollection.api.batch.service;

import ca.bc.gov.educ.studentdatacollection.api.batch.processor.SdcBatchProcessor;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcBatchService;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcBatchStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcFileUpload;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static lombok.AccessLevel.PRIVATE;

/**
 * The type Pen request batch file service.
 *
 * @author OM
 */
@Service
@Slf4j
public class SdcFileService {

  @Getter(PRIVATE)
  private final SdcBatchService sdcBatchService;

  @Getter(PRIVATE)
  private final SdcBatchProcessor sdcBatchProcessor;

  @Getter(PRIVATE)
  private final SdcBatchStudentService sdcBatchStudentService;

  private final StringRedisTemplate stringRedisTemplate;


  @Autowired
  public SdcFileService(SdcBatchService sdcBatchService, SdcBatchProcessor sdcBatchProcessor, SdcBatchStudentService sdcBatchStudentService, StringRedisTemplate stringRedisTemplate) {
    this.sdcBatchService = sdcBatchService;
    this.sdcBatchProcessor = sdcBatchProcessor;
    this.sdcBatchStudentService = sdcBatchStudentService;
    this.stringRedisTemplate = stringRedisTemplate;
  }

  private void runBatchLoad(SdcFileUpload sdcFileUpload){
    log.debug("Uploaded file contents for school ID {} :: {}", sdcFileUpload.getSchoolID(),sdcFileUpload.getFileContents());
    final String redisKey = sdcFileUpload.getSchoolID().concat("::processingFileUpload");
    val valueFromRedis = this.stringRedisTemplate.opsForValue().get(redisKey);
    if (StringUtils.isBlank(valueFromRedis)) { // skip if it is already in redis
      // put it in redis for 5 minutes, it is expected the file processing wont take more than that and if the
      // pod dies in between the scheduler will pick it up and process it again after the lock is released(5
      // minutes).
      this.stringRedisTemplate.opsForValue().set(redisKey, "true", 5, TimeUnit.MINUTES);
      this.getSdcBatchProcessor().processSdcBatchFile(sdcFileUpload);
    } else {
      log.debug("skipping {} record, as it is already processed or being processed.", redisKey);
    }
  }
}
