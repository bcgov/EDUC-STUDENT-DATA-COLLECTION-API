package ca.bc.gov.educ.studentdatacollection.api.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.IOException;
import org.junit.Test;

public class JsonUtilTest {

  @Test
  public void getJsonStringFromObject() throws JsonProcessingException {
    Collection collection = new Collection();
    assertNotNull(JsonUtil.getJsonStringFromObject(collection));
  }

  @Test
  public void getJsonObjectFromString() throws JsonProcessingException {
    Collection collection = new Collection();
    assertNotNull(JsonUtil.getJsonObjectFromString(Collection.class, JsonUtil.getJsonStringFromObject(collection)));
  }

  @Test
  public void getJsonBytesFromObject() throws JsonProcessingException {
    Collection collection = new Collection();
    assertNotNull(JsonUtil.getJsonBytesFromObject(collection));
  }

  @Test
  public void getJsonBytesFromObjectThrowIOException() throws IOException {
    Collection collection = new Collection();
    assertNotNull(JsonUtil.getJsonBytesFromObject(collection));
    assertThrows(IOException.class, () -> JsonUtil.getJsonObjectFromByteArray(School.class, JsonUtil.getJsonBytesFromObject(collection)));
  }
}
