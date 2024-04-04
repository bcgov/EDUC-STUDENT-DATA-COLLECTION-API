package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentLightEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentLightRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletionException;

@Service
@Slf4j
@RequiredArgsConstructor
public class SdcDistrictCollectionStudentSearchService {
    private final SdcSchoolCollectionStudentLightRepository sdcSchoolCollectionStudentLightRepository;
    private final SdcSchoolCollectionRepository sdcSchoolCollectionRepository;

    @Transactional(propagation = Propagation.SUPPORTS)
    public Map<UUID, List<SdcSchoolCollectionStudentLightEntity>> findAllStudentsGroupedBySchoolSynchronous(UUID sdcDistrictCollectionID) {
        try {
            Map<UUID, List<SdcSchoolCollectionStudentLightEntity>> schoolToStudentsMap = new HashMap<>();
            List<SdcSchoolCollectionEntity> schoolCollections = sdcSchoolCollectionRepository.findAllCollectionByDistrictCollectionID(sdcDistrictCollectionID);

            for (SdcSchoolCollectionEntity schoolCollection : schoolCollections) {
                UUID schoolID = schoolCollection.getSchoolID();
                List<SdcSchoolCollectionStudentLightEntity> students = this.sdcSchoolCollectionStudentLightRepository.findAllBySdcSchoolCollectionID(schoolCollection.getSdcSchoolCollectionID());
                schoolToStudentsMap.put(schoolID, students);
            }
            return schoolToStudentsMap;
        } catch (final Throwable ex) {
            throw new CompletionException(ex);
        }
    }
}
