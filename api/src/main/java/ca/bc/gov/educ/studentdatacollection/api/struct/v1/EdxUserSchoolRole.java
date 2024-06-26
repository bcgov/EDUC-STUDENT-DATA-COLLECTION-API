package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdxUserSchoolRole extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 583620260139143932L;

    String edxUserSchoolRoleID;

    @NotNull(message = "edxRoleCode cannot be null.")
    @Size(max = 32, message = "edxRoleCode should be no longer than 32 characters.")
    String edxRoleCode;

    @NotNull(message = "edxUserSchoolID cannot be null.")
    String edxUserSchoolID;
}
