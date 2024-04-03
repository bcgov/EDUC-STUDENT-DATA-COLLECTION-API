package ca.bc.gov.educ.studentdatacollection.api.service.v1;

import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEnrolledProgramEntity;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentEnrolledProgramRepository;
import ca.bc.gov.educ.studentdatacollection.api.rest.RestUtils;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.Year;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class SdcSchoolCollectionStudentHeadcountServiceTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentHeadcountService service;
    @Autowired
    RestUtils restUtils;
    @Autowired
    SdcSchoolCollectionStudentEnrolledProgramRepository sdcSchoolCollectionStudentEnrolledProgramRepository;
    private SdcSchoolCollectionEntity firstSchoolCollection;
    private SdcSchoolCollectionEntity secondSchoolCollection;
    private List<SdcSchoolCollectionStudentEntity> savedStudents;

    @BeforeEach
    void setUp() throws IOException {
        var collection = collectionRepository.save(createMockCollectionEntity());
        var school = this.createMockSchool();
        when(this.restUtils.getSchoolBySchoolID(anyString())).thenReturn(Optional.of(school));
        firstSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        firstSchoolCollection.setUploadDate(null);
        firstSchoolCollection.setUploadFileName(null);

        secondSchoolCollection = createMockSdcSchoolCollectionEntity(collection, UUID.fromString(school.getSchoolId()), UUID.fromString(school.getDistrictId()));
        secondSchoolCollection.setUploadDate(null);
        secondSchoolCollection.setUploadFileName(null);
        secondSchoolCollection.setCreateDate(LocalDateTime.of(Year.now().getValue() - 1, Month.SEPTEMBER, 7, 0, 0));
        sdcSchoolCollectionRepository.saveAll(Arrays.asList(firstSchoolCollection, secondSchoolCollection));

        final File file = new File(
                Objects.requireNonNull(this.getClass().getClassLoader().getResource("sdc-school-students-test-data.json")).getFile()
        );
        final List<SdcSchoolCollectionStudent> entities = new ObjectMapper().readValue(file, new TypeReference<>() {
        });
        var models = entities.stream().map(SdcSchoolCollectionStudentMapper.mapper::toSdcSchoolStudentEntity).toList();
        var students = IntStream.range(0, models.size())
                .mapToObj(i -> {
                    if (i % 2 == 0) {
                        models.get(i).setSdcSchoolCollection(secondSchoolCollection);
                    } else {
                        models.get(i).setSdcSchoolCollection(firstSchoolCollection);
                    }
                    return models.get(i);
                })
                .toList();

        savedStudents = sdcSchoolCollectionStudentRepository.saveAll(students);
    }

    @AfterEach
    void cleanup(){
        collectionRepository.deleteAll();
        sdcSchoolCollectionRepository.deleteAll();
        sdcSchoolCollectionStudentRepository.deleteAll();
    }

    @Test
    void testGetEnrollmentReportValues_WhenCompareIsTrue(){
        var resultsTableWithoutCompare = service.getEnrollmentHeadcounts(firstSchoolCollection, false);
        var allStudentsSection =   resultsTableWithoutCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("All Students") && val.get("title").getCurrentValue().equals("FTE Total")).findAny();
        assertEquals("2.46", allStudentsSection.get().get("Total").getCurrentValue());
        assertNull(allStudentsSection.get().get("Total").getComparisonValue());

        var resultsTableWithCompare = service.getEnrollmentHeadcounts(firstSchoolCollection, true);
        var allStudentsWithCompareSection =   resultsTableWithCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("All Students") && val.get("title").getCurrentValue().equals("FTE Total")).findAny();
        assertEquals("2.46", allStudentsWithCompareSection.get().get("Total").getCurrentValue());
        assertEquals("2.72", allStudentsWithCompareSection.get().get("Total").getComparisonValue());
    }

    @Test
    void testGetCareerProgramReportValues_WhenCompareIsTrue(){

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("40");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });

        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        var resultsTableWithoutCompare = service.getCareerHeadcounts(firstSchoolCollection, false);
        var allStudentsSection =   resultsTableWithoutCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("Career Preparation") && val.get("title").getCurrentValue().equals("Career Preparation")).findAny();
        assertEquals("1", allStudentsSection.get().get("Total").getCurrentValue());
        assertNull(allStudentsSection.get().get("Total").getComparisonValue());

        var resultsTableWithCompare = service.getCareerHeadcounts(firstSchoolCollection, true);
        var allStudentsWithCompareSection =   resultsTableWithCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("Career Preparation") && val.get("title").getCurrentValue().equals("Career Preparation")).findAny();
        assertEquals("1", allStudentsWithCompareSection.get().get("Total").getCurrentValue());
        assertEquals("2", allStudentsWithCompareSection.get().get("Total").getComparisonValue());
    }

    @Test
    void testGetIndigenousProgramReportValues_WhenCompareIsTrue(){

        List<SdcSchoolCollectionStudentEnrolledProgramEntity> enrolledPrograms = new ArrayList<>();

        savedStudents.forEach(student -> {
            var enrolledProg = new SdcSchoolCollectionStudentEnrolledProgramEntity();
            enrolledProg.setSdcSchoolCollectionStudentEntity(student);
            enrolledProg.setEnrolledProgramCode("33");
            enrolledProg.setCreateUser("ABC");
            enrolledProg.setUpdateUser("ABC");
            enrolledProg.setCreateDate(LocalDateTime.now());
            enrolledProg.setUpdateDate(LocalDateTime.now());
            enrolledPrograms.add(enrolledProg);
        });

        sdcSchoolCollectionStudentEnrolledProgramRepository.saveAll(enrolledPrograms);

        var resultsTableWithoutCompare = service.getIndigenousHeadcounts(firstSchoolCollection, false);
        var allStudentsSection =   resultsTableWithoutCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("All Indigenous Support Programs") && val.get("title").getCurrentValue().equals("All Indigenous Support Programs")).findAny();
        assertEquals("2", allStudentsSection.get().get("Total").getCurrentValue());
        assertNull(allStudentsSection.get().get("Total").getComparisonValue());

        var resultsTableWithCompare = service.getIndigenousHeadcounts(firstSchoolCollection, true);
        var allStudentsWithCompareSection =   resultsTableWithCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("section").getCurrentValue().equals("All Indigenous Support Programs") && val.get("title").getCurrentValue().equals("All Indigenous Support Programs")).findAny();
        assertEquals("2", allStudentsWithCompareSection.get().get("Total").getCurrentValue());
        assertEquals("4", allStudentsWithCompareSection.get().get("Total").getComparisonValue());
    }

    @Test
    void testGetBandOfResidenceValues_WhenCompareIsTrue(){
        var resultsTableWithoutCompare = service.getBandResidenceHeadcounts(firstSchoolCollection, false);
        assertEquals(3, resultsTableWithoutCompare.getHeadcountResultsTable().getRows().size());
        var allStudentsWithoutCompareRow = resultsTableWithoutCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("title").getCurrentValue().equals("All Bands & Students")).findAny();
        assertEquals("2.46", allStudentsWithoutCompareRow.get().get("FTE").getCurrentValue());
        assertEquals("2", allStudentsWithoutCompareRow.get().get("Headcount").getCurrentValue());

        var resultsTableWithCompare = service.getBandResidenceHeadcounts(firstSchoolCollection, true);
        var allStudentsWithCompareRow = resultsTableWithCompare.getHeadcountResultsTable().getRows().stream().filter(val -> val.get("title").getCurrentValue().equals("All Bands & Students")).findAny();
        assertEquals(3, resultsTableWithCompare.getHeadcountResultsTable().getRows().size());
        assertEquals("2.46", allStudentsWithCompareRow.get().get("FTE").getCurrentValue());
        assertEquals("2", allStudentsWithCompareRow.get().get("Headcount").getCurrentValue());
        assertEquals("0", allStudentsWithCompareRow.get().get("FTE").getComparisonValue());
        assertEquals("0", allStudentsWithCompareRow.get().get("Headcount").getComparisonValue());
    }

}
