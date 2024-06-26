package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SdcSchoolCollection1701Users extends BaseRequest {
    private String schoolDisplayName;
    private Set<String> emails;
}
