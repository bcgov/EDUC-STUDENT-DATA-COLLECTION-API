package ca.bc.gov.educ.studentdatacollection.api.validator;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.BandCodeEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.BandCode;
import ca.bc.gov.educ.studentdatacollection.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class BandCodeValidator {

  private static final String BAND_CODE = "bandCode";
  private static final String EFFECTIVE_DATE = "effectiveDate";
  private static final String EXPIRY_DATE = "expiryDate";

  @Getter(AccessLevel.PRIVATE)
  private final CodeTableService codeTableService;

    public BandCodeValidator(CodeTableService codeTableService) {
        this.codeTableService = codeTableService;
    }

    public List<FieldError> validatePayload(BandCode bandCode) {
    final List<FieldError> apiValidationErrors = new ArrayList<>();
    var bandCodeList = codeTableService.getAllBandCodes().stream().map(BandCodeEntity::getBandCode).toList();
    if(!bandCodeList.contains(bandCode.getBandCode())) {
      apiValidationErrors.add(ValidationUtil.createFieldError(BAND_CODE, BAND_CODE, bandCode.getBandCode(), "bandCode was not found in the list of Bands."));
    }
    try{
      LocalDateTime.parse(bandCode.getEffectiveDate());
    }catch (Exception e){
      apiValidationErrors.add(ValidationUtil.createFieldError(EFFECTIVE_DATE, EFFECTIVE_DATE, bandCode.getEffectiveDate(), "effectiveDate is not a valid date."));
    }
    try{
      LocalDateTime.parse(bandCode.getExpiryDate());
    }catch (Exception e){
      apiValidationErrors.add(ValidationUtil.createFieldError(EXPIRY_DATE, EXPIRY_DATE, bandCode.getExpiryDate(), "expiryDate is not a valid date."));
    }
    return apiValidationErrors;
  }
}
