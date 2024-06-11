package ca.bc.gov.educ.studentdatacollection.api.struct;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EmailNotification {
  @NotNull(message = "fromEmail can not be null.")
  private String fromEmail;
  @NotNull(message = "toEmail can not be null.")
  private String toEmail;
  @NotNull(message = "subject can not be null.")
  private String subject;

  @NotNull(message = "templateName can not be null.")
  private String templateName;

  @NotNull(message = "emailFields can not be null.")
  private Map<String, String> emailFields;
}

