package ca.bc.gov.educ.studentdatacollection.api;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.School;
import ca.bc.gov.educ.studentdatacollection.api.support.StudentDataCollectionTestUtils;
import ca.bc.gov.educ.studentdatacollection.api.support.TestRedisConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootTest(classes = {TestRedisConfiguration.class, StudentDataCollectionApiApplication.class})
@ActiveProfiles("test")
@AutoConfigureMockMvc
public abstract class BaseStudentDataCollectionAPITest {

  @Autowired
  protected StudentDataCollectionTestUtils studentDataCollectionTestUtils;

  @AfterEach
  public void resetState() {
    this.studentDataCollectionTestUtils.cleanDB();
  }

  public CollectionEntity createCollectionEntity(){
    CollectionEntity sdcEntity = new CollectionEntity();
    sdcEntity.setCollectionCode("SEPTEMBER");
    sdcEntity.setOpenDate(LocalDateTime.now());
    sdcEntity.setCloseDate(null);
    sdcEntity.setCreateUser("ABC");
    sdcEntity.setCreateDate(LocalDateTime.now());
    sdcEntity.setUpdateUser("ABC");
    sdcEntity.setUpdateDate(LocalDateTime.now());
    return sdcEntity;
  }

  public School createMockSchool() {
    final School school = new School();
    school.setSchoolId(UUID.randomUUID().toString());
    school.setDisplayName("Marco's school");
    school.setMincode("66510518");
    school.setOpenedDate("1964-09-01T00:00:00");
    return school;
  }
}
