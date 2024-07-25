package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum SdcSchoolStudentStatus {
  LOADED("LOADED"),
  ERROR("ERROR"),
  INFO_WARNING("INFOWARN"),
  FUNDING_WARNING("FUNDWARN"),
  VERIFIED("VERIFIED"),
  FIXABLE("FIXABLE"),
  DELETED("DELETED"),
  DEMOG_UPD("DEMOG_UPD"),
  COMPLETED("COMPLETED");


  private static final Map<String, SdcSchoolStudentStatus> codeMap = new HashMap<>();

  static {
    for (SdcSchoolStudentStatus status : values()) {
      codeMap.put(status.getCode(), status);
    }
  }

  private final String code;

  SdcSchoolStudentStatus(String code) {
    this.code = code;
  }

  public static SdcSchoolStudentStatus valueOfCode(String code) {
    return codeMap.get(code);
  }

  @Override
  public String toString() {
    return this.getCode();
  }
}
