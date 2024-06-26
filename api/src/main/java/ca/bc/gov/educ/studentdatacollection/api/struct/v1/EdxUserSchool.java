package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdxUserSchool extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 583620260139143932L;

    String edxUserSchoolID;
    String edxUserID;
    UUID schoolID;
    String expiryDate;

    private List<EdxUserSchoolRole> edxUserSchoolRoles;
}
