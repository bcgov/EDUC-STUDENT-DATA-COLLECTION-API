package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EdxUser extends BaseRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 583620260139143932L;

    String edxUserID;

    @NotNull(message = "Digital Identity ID cannot be null")
    String digitalIdentityID;

    @Size(max = 255, message = "First Name can have max 255 characters")
    @NotNull(message = "First Name cannot be null")
    String firstName;

    @Size(max = 255, message = "Last Name can have max 255 characters")
    @NotNull(message = "Last Name cannot be null")
    String lastName;

    private List<EdxUserSchool> edxUserSchools;
    private List<EdxUserDistrict> edxUserDistricts;

    @Size(max = 255)
    @NotNull(message = "Email cannot be null")
    @Email(message = "Email address should be a valid email address")
    String email;
}
