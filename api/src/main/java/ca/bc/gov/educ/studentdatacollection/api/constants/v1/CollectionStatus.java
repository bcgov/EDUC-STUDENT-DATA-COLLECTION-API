package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

public enum CollectionStatus {

  INPROGRESS("INPROGRESS"),
  PROVDUPES("PROVDUPES"),

  COMPLETED("COMPLETED");

  @Getter
  private final String code;

  CollectionStatus(final String code) {
    this.code = code;
  }
}
