package ca.bc.gov.educ.studentdatacollection.api.util;

import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidPayloadException;
import ca.bc.gov.educ.studentdatacollection.api.exception.errors.ApiError;
import lombok.val;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

public class ValidationUtil {

  private ValidationUtil(){

  }

  public static void validatePayload(Supplier<List<FieldError>> validator) {
    val validationResult = validator.get();
    if (!validationResult.isEmpty()) {
      ApiError error = ApiError.builder().timestamp(LocalDateTime.now()).message("Payload contains invalid data.").status(BAD_REQUEST).build();
      error.addValidationErrors(validationResult);
      throw new InvalidPayloadException(error);
    }
  }
}
