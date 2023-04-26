package ca.bc.gov.educ.studentdatacollection.api.mappers.v1;

import ca.bc.gov.educ.studentdatacollection.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.*;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {LocalDateTimeMapper.class})
public interface CodeTableMapper {

    CodeTableMapper mapper = Mappers.getMapper(CodeTableMapper.class);

    EnrolledProgramCode toStructure(EnrolledProgramCodeEntity entity);
    CareerProgramCode toStructure(CareerProgramCodeEntity entity);
    HomeLanguageSpokenCode toStructure(HomeLanguageSpokenCodeEntity entity);
    BandCode toStructure(BandCodeEntity entity);
    SchoolFundingCode toStructure(SchoolFundingCodeEntity entity);
    EnrolledGradeCode toStructure(EnrolledGradeCodeEntity entity);
}
