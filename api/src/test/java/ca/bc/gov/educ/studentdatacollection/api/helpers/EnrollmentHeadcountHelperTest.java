
package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.CollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcDistrictCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.EnrollmentHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = StudentDataCollectionApiApplication.class)
class EnrollmentHeadcountHelperTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository studentRepository;

    private EnrollmentHeadcountHelper helper;

    private SdcDistrictCollectionEntity mockDistrictCollectionEntity;

    @Autowired
    private SdcSchoolCollectionRepository schoolCollectionRepository;

    @Autowired
    RestUtils restUtils;

    @Autowired
    SdcDistrictCollectionRepository sdcDistrictCollectionRepository;

    @BeforeEach
    void setUp() throws IOException {
        CollectionEntity collection = collectionRepository.save(createMockCollectionEntity());
        var districtID = UUID.randomUUID();
        mockDistrictCollectionEntity = sdcDistrictCollectionRepository.save(createMockSdcDistrictCollectionEntity(collection, districtID));


        var school1 = createMockSchool();
        school1.setDisplayName("School1");
        school1.setMincode("0000001");
        school1.setDistrictId(districtID.toString());
        var school2 = createMockSchool();
        school2.setDisplayName("School2");
        school2.setMincode("0000002");
        school2.setDistrictId(districtID.toString());

        when(this.restUtils.getSchoolBySchoolID(school1.getSchoolId())).thenReturn(Optional.of(school1));
        when(this.restUtils.getSchoolBySchoolID(school2.getSchoolId())).thenReturn(Optional.of(school2));

        var firstSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school1.getSchoolId()));
        firstSchool.setUploadDate(null);
        firstSchool.setUploadFileName(null);
        firstSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        var secondSchool = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school2.getSchoolId()));
        secondSchool.setUploadDate(null);
        secondSchool.setUploadFileName(null);
        secondSchool.setSdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        secondSchool.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchool, secondSchool));


        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchool);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchool);
                    }
                    return models.get(i);
                })
                .toList();

        sdcSchoolCollectionStudentRepository.saveAll(students);
    }

    @AfterEach
    void cleanup(){
        studentRepository.deleteAll();
        schoolCollectionRepository.deleteAll();
        sdcDistrictCollectionRepository.deleteAll();
        collectionRepository.deleteAll();
    }

    @Test
    void testConvertHeadcountResults_ShouldReturnTableContents(){

        helper = new EnrollmentHeadcountHelper(schoolCollectionRepository, studentRepository, restUtils, sdcDistrictCollectionRepository);

        List<EnrollmentHeadcountResult> result = sdcSchoolCollectionStudentRepository.getEnrollmentHeadcountsBySchoolIdAndBySdcDistrictCollectionId(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        helper.setGradeCodesForDistricts();
        HeadcountResultsTable actualResultsTable = helper.convertEnrollmentBySchoolHeadcountResults(mockDistrictCollectionEntity.getSdcDistrictCollectionID(), result);
        var schoolSection = actualResultsTable.getRows().stream().map(row -> row.get("section")).toList();

        var titles = actualResultsTable.getRows().stream().filter(row -> row.get("section").getCurrentValue().equals("All Schools") &&
                row.get("title").getCurrentValue().equals("FTE Total")).findAny();

        assert(schoolSection.stream().anyMatch(val -> val.getCurrentValue().equals("0000002 - School2")));
        assert(schoolSection.stream().anyMatch(val -> val.getCurrentValue().equals("0000001 - School1")));
        assert(schoolSection.stream().anyMatch(val -> val.getCurrentValue().equals("All Schools")));

        assert(titles.isPresent());
        assertEquals("1.8000", titles.get().get("01").getCurrentValue());
        assertEquals("0.7900", titles.get().get("02").getCurrentValue());
        assertEquals("0.7900", titles.get().get("10").getCurrentValue());
        assertEquals("1.8000", titles.get().get("11").getCurrentValue());
        assertEquals("5.1800", titles.get().get("Total").getCurrentValue());

    }

}
