package ca.bc.gov.educ.studentdatacollection.api.utils;

import ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.Collection;
import ca.bc.gov.educ.studentdatacollection.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
    try {
      JsonUtil.getJsonObjectFromByteArray(SchoolTombstone.class, JsonUtil.getJsonBytesFromObject(collection));
    } catch (IOException e) {
      String expectedValue = "Cannot construct instance of `ca.bc.gov.educ.studentdatacollection.api.struct.external.institute.v1.School` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)\n"
          + " at [Source: (byte[])\"{\"createUser\":null,\"updateUser\":null,\"createDate\":null,\"updateDate\":null,\"collectionID\":null,\"collectionCode\":null,\"openDate\":null,\"closeDate\":null}\"; line: 1, column: 2]";
      assertEquals(expectedValue, e.getMessage());
    }
  }
}
