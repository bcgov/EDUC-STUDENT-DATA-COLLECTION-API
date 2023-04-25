package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;

import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Service
public class ValidationRulesService {
    @Getter(PRIVATE)
    private final SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository;

    public ValidationRulesService(SdcSchoolCollectionStudentRepository sdcSchoolStudentRepository) {

        this.sdcSchoolStudentRepository = sdcSchoolStudentRepository;
    }

    public Long getDuplicatePenCount(String sdcSchoolID, String studentPen) {
        return sdcSchoolStudentRepository.countForDuplicateStudentPENs(UUID.fromString(sdcSchoolID), studentPen);
    }
}
