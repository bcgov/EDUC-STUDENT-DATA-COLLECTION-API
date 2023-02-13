package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseRequest;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.apache.commons.lang3.StringUtils;

/**
 * The type Request util.
 */
public class RequestUtil {
  private RequestUtil() {
  }

  /**
   * set audit data to the object.
   *
   * @param baseRequest The object which will be persisted.
   */
  public static void setAuditColumnsForCreate(@NotNull BaseRequest baseRequest) {
    if (StringUtils.isBlank(baseRequest.getCreateUser())) {
      baseRequest.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    }
    baseRequest.setCreateDate(LocalDateTime.now().toString());
    setAuditColumnsForUpdate(baseRequest);
  }

  /**
   * set audit data to the object.
   *
   * @param baseRequest The object which will be persisted.
   */
  public static void setAuditColumnsForUpdate(@NotNull BaseRequest baseRequest) {
    if (StringUtils.isBlank(baseRequest.getUpdateUser())) {
      baseRequest.setUpdateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    }
    baseRequest.setUpdateDate(LocalDateTime.now().toString());
  }
}
