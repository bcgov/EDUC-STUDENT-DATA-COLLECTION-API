package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

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
public class HomeLanguageSpokenCode implements Serializable {

    private static final long serialVersionUID = 1L;

    private String homeLanguageSpokenCode;

    private String label;

    private String description;

    private Integer displayOrder;

    private String effectiveDate;

    private String expiryDate;
}
