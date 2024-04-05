package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.BadNameValues;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class ValidationUtil {

  private static final String INVALID_CHARS = "'-.";

  private ValidationUtil(){

  }

  public static FieldError createFieldError(String fieldName, Object rejectedValue, String message) {
    return new FieldError("sdcSchoolCollection", fieldName, rejectedValue, false, null, null, message);
  }

  public static FieldError createFieldError(String objectName, String fieldName, Object rejectedValue, String message) {
    return new FieldError(objectName, fieldName, rejectedValue, false, null, null, message);
  }

  public static void validatePayload(Supplier<List<FieldError>> validator) {
    val validationResult = validator.get();
    if (!validationResult.isEmpty()) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }

  public static boolean containsBadValue(String name) {
    return StringUtils.isNotEmpty(name) && BadNameValues.findByValue(name).isPresent();
  }

  public static boolean containsInvalidChars(String name) {
    Pattern pattern = Pattern.compile("^[a-z\\-.'\\s]+$", Pattern.CASE_INSENSITIVE);
    return StringUtils.isNotEmpty(name) && (!pattern.matcher(name).matches() || name.replaceAll("[" + INVALID_CHARS + "]","").isEmpty());
  }
}
