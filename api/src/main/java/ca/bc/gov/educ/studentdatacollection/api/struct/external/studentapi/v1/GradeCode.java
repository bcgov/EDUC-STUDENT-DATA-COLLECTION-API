package ca.bc.gov.educ.studentdatacollection.api.struct.external.studentapi.v1;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuppressWarnings("squid:S1700")
public class GradeCode {
    private static final long serialVersionUID = 8115561492500492122L;
    /**
     * The Grade code.
     */
    String gradeCode;
    /**
     * The Label.
     */
    String label;
    /**
     * The Description.
     */
    String description;
    /**
     * The Display order.
     */
    Integer displayOrder;
    /**
     * The Effective date.
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime effectiveDate;
    /**
     * The Expiry date.
     */
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonSerialize(using = LocalDateTimeSerializer.class)
    LocalDateTime expiryDate;
}
