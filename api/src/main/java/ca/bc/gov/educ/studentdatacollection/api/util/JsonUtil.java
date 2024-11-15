package ca.bc.gov.educ.studentdatacollection.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Optional;

/**
 * The type Json util.
 *
 * @author OM
 */
@Slf4j
public class JsonUtil {
  public static final ObjectMapper mapper = new ObjectMapper();
  /**
   * Instantiates a new Json util.
   */
  private JsonUtil(){
  }

  /**
   * Gets json string from object.
   *
   * @param payload the payload
   * @return the json string from object
   * @throws JsonProcessingException the json processing exception
   */
  public static String getJsonStringFromObject(Object payload) throws JsonProcessingException {
    return mapper.writeValueAsString(payload);
  }

  /**
   * Gets json object from string.
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the payload
   * @return the json object from string
   * @throws JsonProcessingException the json processing exception
   */
  public static <T> T getJsonObjectFromString(Class<T> clazz,  String payload) throws JsonProcessingException {
    return mapper.readValue(payload, clazz);
  }

  /**
   * Gets json object from string.
   *
   * @param <T>     the type parameter
   * @param clazz   the clazz
   * @param payload the payload
   * @return the json object from string
   * @throws IOException the io exception
   */
  public static <T> T getJsonObjectFromByteArray(Class<T> clazz,  byte[] payload) throws IOException {
    return mapper.readValue(payload, clazz);
  }

  /**
   * Get json bytes from object byte [ ].
   *
   * @param payload the payload
   * @return the byte [ ]
   * @throws JsonProcessingException the json processing exception
   */
  public static byte[] getJsonBytesFromObject(final Object payload) throws JsonProcessingException {
    return new ObjectMapper().writeValueAsBytes(payload);
  }

  /**
   * Get json string optional.
   *
   * @param payload the payload
   * @return the optional
   */
  public static String getJsonString(Object payload) throws JsonProcessingException {
    try {
      return mapper.writeValueAsString(payload);
    }catch(JsonProcessingException e){
      log.error("Error writing JSON as String :: {}", e);
      throw e;
    }
  }
}
