package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import java.beans.Expression;
import java.beans.Statement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.springframework.util.StringUtils.capitalize;

/**
 * The type Transform util.
 */
public class TransformUtil {
  private TransformUtil() {
  }

  /**
   * Uppercase fields t.
   *
   * @param <T>    the type parameter
   * @param claz the claz
   * @return the t
   */
  public static <T> T uppercaseFields(T claz) {
    var clazz = claz.getClass();
    List<Field> fields = new ArrayList<>();
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      fields.addAll(Arrays.asList(superClazz.getDeclaredFields()));
      superClazz = superClazz.getSuperclass();
    }
    fields.forEach(field -> TransformUtil.transformFieldToUppercase(field, claz));
    return claz;
  }

  /**
   * Is uppercase field boolean.
   *
   * @param clazz     the clazz
   * @param fieldName the field name
   * @return the boolean
   */
  public static boolean isUppercaseField(Class<?> clazz, String fieldName) {
    var superClazz = clazz;
    while (!superClazz.equals(Object.class)) {
      try {
        Field field = superClazz.getDeclaredField(fieldName);
        return field.getAnnotation(UpperCase.class) != null;
      } catch (NoSuchFieldException e) {
        superClazz = superClazz.getSuperclass();
      }
    }
    return false;
  }

  /**
   * @param string - the four didgit float string, eg: "0800"
   * @return a Double, eg: 8.00
   */
  public static Double hundredthDecimalAsIntegerStringToDouble(String string) {
    if (string == null) { return 0D; }
    return Double.parseDouble(string) / 100;
  }

  private static <T> void transformFieldToUppercase(Field field, T claz) {
    if (!field.getType().equals(String.class)) {
      return;
    }

    if (field.getAnnotation(UpperCase.class) != null) {
      try {
        var fieldName = capitalize(field.getName());
        var expr = new Expression(claz, "get" + fieldName, new Object[0]);
        var entityFieldValue = (String) expr.getValue();
        if (entityFieldValue != null) {
          var stmt = new Statement(claz, "set" + fieldName, new Object[]{entityFieldValue.toUpperCase()});
          stmt.execute();
        }
      } catch (Exception ex) {
        throw new StudentDataCollectionAPIRuntimeException(ex.getMessage());
      }
    }
  }

  public static List<String> splitIntoChunks(String text, int numberOfCharacters) {
    String[] results = text.split("(?<=\\G.{" + numberOfCharacters + "})");

    return Arrays.asList(results);
  }
}
