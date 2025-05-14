package ca.bc.gov.educ.studentdatacollection.api.struct.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.util.UpperCase;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ToString(callSuper = true)
public class SdcSchoolCollectionStudentShallow implements Serializable {

  private static final long serialVersionUID = 1L;

  private String sdcSchoolCollectionStudentID;

  private String sdcSchoolCollectionID;

  private String sdcDistrictCollectionID;

  private String legalFirstName;

  private String legalMiddleNames;

  private String legalLastName;

  private String usualFirstName;

  private String usualMiddleNames;

  private String usualLastName;

  private String dob;

  private String gender;

  private String specialEducationCategoryCode;

  private String schoolFundingCode;

  private String nativeAncestryInd;

  private String enrolledGradeCode;

  private String schoolID;

  private String bandCode;

  private String isAdult;

  private String isSchoolAged;

  private BigDecimal fte;

  private String assignedPen;

  private String assignedStudentId;

}
