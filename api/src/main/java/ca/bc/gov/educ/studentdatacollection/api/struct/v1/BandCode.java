package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@SuppressWarnings("squid:S1700")
public class BandCode extends BaseRequest implements Serializable {
  private static final long serialVersionUID = 6118916290604876032L;

  @Size(max = 10)
  @NotNull(message = "bandCode can not be null.")
  private String bandCode;

  @Size(max = 100)
  @NotNull(message = "label can not be null.")
  private String label;

  @Size(max = 255)
  @NotNull(message = "description can not be null.")
  private String description;

  @NotNull(message = "displayOrder can not be null.")
  private Integer displayOrder;

  @NotNull(message = "effectiveDate can not be null.")
  private String effectiveDate;

  @NotNull(message = "expiryDate can not be null.")
  private String expiryDate;

}
