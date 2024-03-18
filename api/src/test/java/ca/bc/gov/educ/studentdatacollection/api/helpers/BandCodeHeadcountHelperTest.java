package ca.bc.gov.educ.studentdatacollection.api.helpers;
import ca.bc.gov.educ.studentdatacollection.api.BaseStudentDataCollectionAPITest;
import ca.bc.gov.educ.studentdatacollection.api.StudentDataCollectionApiApplication;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.*;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.BandCodeRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.CodeTableService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.BandResidenceHeadcountResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = StudentDataCollectionApiApplication.class)
class BandCodeHeadcountHelperTest extends BaseStudentDataCollectionAPITest {

    @Autowired
    private SdcSchoolCollectionStudentRepository studentRepository;

    private SdcSchoolCollectionEntity schoolCollection;
    private BandResidenceHeadcountHelper helper;

    @Autowired
    private SdcSchoolCollectionRepository schoolCollectionRepository;

    @Autowired
    BandCodeRepository bandCodeRepository;

    @Autowired
    CodeTableService codeTableService;

    @BeforeEach
    void setUp() {
        helper.setBandRowTitles(new HashMap<>());
        CollectionEntity collectionEntity = createMockCollectionEntity();
        collectionRepository.save(collectionEntity);
        UUID schoolID = UUID.randomUUID();
        schoolCollection = createMockSdcSchoolCollectionEntity(collectionEntity, schoolID, UUID.randomUUID());
        schoolCollectionRepository.save(schoolCollection);
    }

    @AfterEach
    void cleanup(){
        studentRepository.deleteAll();
        schoolCollectionRepository.deleteAll();
        bandCodeRepository.deleteAll();
        collectionRepository.deleteAll();
    }

    @Test
    void testGetBandCodeTitlesFromCollection_ShouldCorrectlySetRowTitles() {

        saveStudentsWithBandCodes();
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        Map<String, String> bandRowTitlesMap = helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());
        assertEquals(2, bandRowTitlesMap.size());
        assertEquals("0700 - BOOTHROYD - AFA", bandRowTitlesMap.get("0700"));
        assertEquals("0600 - SPLATSIN", bandRowTitlesMap.get("0600"));
    }

    @Test
    void testBandCodeTitlesFromCollection_ShouldNotSetAnyRowTitles() {

        SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(schoolCollection);
        student.setFte(new BigDecimal("1.0000"));
        student.setBandCode(null);
        studentRepository.save(student);
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        Map<String, String> bandRowTitlesMap = helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());
        assertEquals(0, bandRowTitlesMap.size());
    }

    @Test
    void testGetHeadcountHeaders_ShouldReturnListOfHeaders() {

        saveStudentsWithBandCodes();
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        List<HeadcountHeader> expectedHeadcountHeaderList = new ArrayList<>();
        HeadcountHeader header1 = new HeadcountHeader("0600 - SPLATSIN", null, List.of("Headcount", "FTE"), new HashMap<>());
        HeadcountHeader header2 = new HeadcountHeader("0700 - BOOTHROYD - AFA", null, List.of("Headcount", "FTE"), new HashMap<>());
        expectedHeadcountHeaderList.add(header1);
        expectedHeadcountHeaderList.add(header2);

        List<HeadcountHeader> returnedList = helper.getHeadcountHeaders(new ArrayList<>(List.of(schoolCollection.getSdcSchoolCollectionID())));

        assertEquals(expectedHeadcountHeaderList, returnedList);
    }

    @Test
    void testConvertHeadcountResults_ShouldReturnTableContents(){

        saveStudentsWithBandCodes();
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        helper.setBandRowTitles(helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID()));

        List<BandResidenceHeadcountResult> result = studentRepository.getBandResidenceHeadcountsBySchoolId(schoolCollection.getSdcSchoolCollectionID());
        HeadcountResultsTable actualResultsTable = helper.convertBandHeadcountResults(result);

        assertTrue(actualResultsTable.getHeaders().contains("Headcount"));
        assertTrue(actualResultsTable.getHeaders().contains("FTE"));

        assertEquals("0600 - SPLATSIN", actualResultsTable.getRows().get(0).get("title").getCurrentValue());
        assertEquals("1.41", actualResultsTable.getRows().get(0).get("FTE").getCurrentValue());
        assertEquals("2", actualResultsTable.getRows().get(0).get("Headcount").getCurrentValue());

        assertEquals("0700 - BOOTHROYD - AFA", actualResultsTable.getRows().get(1).get("title").getCurrentValue());
        assertEquals("0.50", actualResultsTable.getRows().get(1).get("FTE").getCurrentValue());
        assertEquals("1", actualResultsTable.getRows().get(1).get("Headcount").getCurrentValue());

        assertEquals("All Bands & Students", actualResultsTable.getRows().get(2).get("title").getCurrentValue());
        assertEquals("1.91", actualResultsTable.getRows().get(2).get("FTE").getCurrentValue());
        assertEquals("3", actualResultsTable.getRows().get(2).get("Headcount").getCurrentValue());
    }

    void saveStudentsWithBandCodes(){
        SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(schoolCollection);
        student1.setBandCode("0600");
        student1.setFte(new BigDecimal("0.4100"));
        studentRepository.save(student1);

        SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(schoolCollection);
        student2.setBandCode("0700");
        student2.setFte(new BigDecimal("0.5000"));
        studentRepository.save(student2);

        SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(schoolCollection);
        student3.setBandCode("0600");
        student3.setFte(new BigDecimal("1.0000"));
        studentRepository.save(student3);
    }

}
