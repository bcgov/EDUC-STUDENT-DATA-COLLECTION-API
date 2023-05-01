package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.properties.ApplicationProperties;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BaseRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
   * set audit data to the object if audit (createUser/createDate) is blank
   *
   * @param baseRequest The object which will be persisted.
   */
  public static void setAuditColumnsForCreateIfBlank(@NotNull BaseRequest baseRequest) {
    if (StringUtils.isBlank(baseRequest.getCreateUser())) {
      baseRequest.setCreateUser(ApplicationProperties.STUDENT_DATA_COLLECTION_API);
    }
    if (StringUtils.isBlank(baseRequest.getCreateDate())) {
      baseRequest.setCreateDate(LocalDateTime.now().toString());
    }
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

  /**
   * Get the Sort.Order list from JSON string
   *
   * @param sortCriteriaJson The sort criterio JSON
   * @param objectMapper     The object mapper
   * @param sorts            The Sort.Order list
   * @throws JsonProcessingException the json processing exception
   */
  public static void getSortCriteria(String sortCriteriaJson, ObjectMapper objectMapper, List<Sort.Order> sorts) throws JsonProcessingException {
    if (StringUtils.isNotBlank(sortCriteriaJson)) {
      Map<String, String> sortMap = objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
      });
      sortMap.forEach((k, v) -> {
        if ("ASC".equalsIgnoreCase(v)) {
          sorts.add(new Sort.Order(Sort.Direction.ASC, k));
        } else {
          sorts.add(new Sort.Order(Sort.Direction.DESC, k));
        }
      });
    }
  }
}
