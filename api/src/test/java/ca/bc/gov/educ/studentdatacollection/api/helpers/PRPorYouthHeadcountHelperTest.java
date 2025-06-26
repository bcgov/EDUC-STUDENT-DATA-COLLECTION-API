package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.constants.v1.FacilityTypeCodes;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.PRPorYouthHeadcountResult;
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

import static org.mockito.Mockito.when;

@SpringBootTest(classes = StudentDataCollectionApiApplication.class)
class PRPorYouthHeadcountHelperTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository studentRepository;

    @Autowired
    private SdcSchoolCollectionStudentEnrolledProgramRepository enrolledProgramRepository;

    private PRPorYouthHeadcountHelper helper;

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
        school1.setDisplayName("Youth School");
        school1.setMincode("1000001");
        school1.setDistrictId(districtID.toString());
        school1.setFacilityTypeCode(FacilityTypeCodes.YOUTH.getCode());
        var school2 = createMockSchool();
        school2.setDisplayName("PRP School");
        school2.setMincode("1000002");
        school2.setDistrictId(districtID.toString());
        school2.setFacilityTypeCode(FacilityTypeCodes.SHORT_PRP.getCode());

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
    void testConvertHeadcountResultsToSchoolGradeTable_ShouldReturnTableContents(){
        helper = new PRPorYouthHeadcountHelper(schoolCollectionRepository, studentRepository, sdcDistrictCollectionRepository, restUtils);
        helper.setGradeCodesForDistricts();

        var allSchools = sdcSchoolCollectionRepository.findAllBySdcDistrictCollectionID(mockDistrictCollectionEntity.getSdcDistrictCollectionID());
        List<UUID> allSchoolIds = allSchools.stream().map(SdcSchoolCollectionEntity::getSchoolID).toList();
        List<UUID> youthSchoolIds = allSchools.stream().map(SdcSchoolCollectionEntity::getSchoolID).filter(schoolID -> {
            var school = restUtils.getSchoolBySchoolID(schoolID.toString());
            return school.isPresent() && FacilityTypeCodes.YOUTH.getCode().equals(school.get().getFacilityTypeCode());
        }).toList();
        List<UUID> shortPrpSchoolIds = allSchools.stream().map(SdcSchoolCollectionEntity::getSchoolID).filter(schoolID -> {
            var school = restUtils.getSchoolBySchoolID(schoolID.toString());
            return school.isPresent() && FacilityTypeCodes.SHORT_PRP.getCode().equals(school.get().getFacilityTypeCode());
        }).toList();
        List<UUID> longPrpSchoolIds = Collections.emptyList();

        List<PRPorYouthHeadcountResult> result = studentRepository.getYouthPRPHeadcountsBySdcDistrictCollectionIdGroupBySchoolId(
                mockDistrictCollectionEntity.getSdcDistrictCollectionID(),
                allSchoolIds,
                youthSchoolIds,
                shortPrpSchoolIds,
                longPrpSchoolIds
        );
        HeadcountResultsTable actualResultsTable = helper.convertHeadcountResultsToSchoolGradeTable(mockDistrictCollectionEntity.getSdcDistrictCollectionID(), result);
        var schoolSection = actualResultsTable.getRows().stream().map(row -> row.get("section")).toList();
        assert(schoolSection.stream().anyMatch(val -> val.getCurrentValue().contains("All Schools")));
    }
}
