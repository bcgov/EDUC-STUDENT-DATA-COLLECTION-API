package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdxUserDistrictRole extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 583620260139143932L;

    String edxUserDistrictRoleID;
    String edxRoleCode;
    String edxUserDistrictID;
}
