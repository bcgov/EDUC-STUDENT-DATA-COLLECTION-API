package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentLightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcDistrictCollectionStudentSearchService {
    private final SdcSchoolCollectionStudentLightRepository sdcSchoolCollectionStudentLightRepository;

    @Transactional(propagation = Propagation.SUPPORTS)
    public List<SdcSchoolCollectionStudentLightEntity> findAllStudentsLightSynchronous(UUID collectionID) {
        try {
            // TODO we need to get all the sdcSchoolCollectionID from collectionID (sdcDistrictCollectionID)
            return this.sdcSchoolCollectionStudentLightRepository.findAllBySdcSchoolCollectionID(collectionID);
        } catch (final Throwable ex) {
            throw new CompletionException(ex);
        }
    }
}
