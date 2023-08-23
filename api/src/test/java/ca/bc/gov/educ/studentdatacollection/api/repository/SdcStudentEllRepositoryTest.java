package ca.bc.gov.educ.studentdatacollection.api.repository;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcStudentEllEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcStudentEllRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@SpringBootTest(classes = {StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SdcStudentEllRepositoryTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcStudentEllRepository sdcStudentEllRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddEllStudent_shouldSave() {
        var newEntity = getSdcStudentEllEntity();
        sdcStudentEllRepository.save(newEntity);

        var savedEntity = sdcStudentEllRepository.findByStudentID(newEntity.getStudentID());

        assertNotNull(savedEntity.get());
        assertSame(savedEntity.get().getYearsInEll(), newEntity.getYearsInEll());
    }

    private SdcStudentEllEntity getSdcStudentEllEntity(){
        SdcStudentEllEntity entity = new SdcStudentEllEntity();
        entity.setStudentID(UUID.randomUUID());
        entity.setYearsInEll(1);
        entity.setCreateUser("ABC");
        entity.setCreateDate(LocalDateTime.now());
        entity.setUpdateUser("ABC");
        entity.setUpdateDate(LocalDateTime.now());
        return entity;
    }
}
