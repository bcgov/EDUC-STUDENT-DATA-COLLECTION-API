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
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountResultsTable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.IntStream;

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


        List<String> expectedTitles = Arrays.asList("0704 - KANAKA BAR", "2411 - ANSPAYAXW");
        helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());
        assertEquals(expectedTitles, new ArrayList<>(BandResidenceHeadcountHelper.getBandRowTitles().values()));
    }

    @Test
    void testBandCodeTitlesFromCollection_ShouldNotSetAnyRowTitles() {

        SdcSchoolCollectionStudentEntity student = createMockSchoolStudentEntity(schoolCollection);
        student.setFte(new BigDecimal("1.0000"));
        student.setBandCode(null);
        studentRepository.save(student);
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());
        List<String> actualRowTitles = new ArrayList<>(BandResidenceHeadcountHelper.getBandRowTitles().values());
        assertEquals(0, actualRowTitles.size());

    }

    @Test
    void testGetHeadcountHeaders_ShouldReturnListOfHeaders() {

        saveStudentsWithBandCodes();
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        List<HeadcountHeader> expectedHeadcountHeaderList = new ArrayList<>();
        HeadcountHeader header1 = new HeadcountHeader("0704 - KANAKA BAR", null, List.of("Indigenous Language and Culture", "Headcount", "FTE"), new HashMap<>());
        HeadcountHeader header2 = new HeadcountHeader("2411 - ANSPAYAXW", null, List.of("Indigenous Language and Culture", "Headcount", "FTE"), new HashMap<>());
        expectedHeadcountHeaderList.add(header1);
        expectedHeadcountHeaderList.add(header2);

        List<HeadcountHeader> returnedList = helper.getHeadcountHeaders(schoolCollection.getSdcSchoolCollectionID());

        assertEquals(expectedHeadcountHeaderList, returnedList);
    }

    @Test
    void testConvertHeadcountResults_ShouldReturnTableContents(){

        saveStudentsWithBandCodes();
        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

        helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());

        List<BandResidenceHeadcountResult> result = studentRepository.getBandResidenceHeadcountsBySchoolId(schoolCollection.getSdcSchoolCollectionID());
        HeadcountResultsTable actualResultsTable = helper.convertBandHeadcountResults(result);

        System.out.println(actualResultsTable);
        assertTrue(actualResultsTable.getHeaders().contains("Indigenous Language and Culture"));
        assertTrue(actualResultsTable.getHeaders().contains("Headcount"));
        assertTrue(actualResultsTable.getHeaders().contains("FTE"));

        assertEquals("0704 - KANAKA BAR", actualResultsTable.getRows().get(0).get("title").getCurrentValue());
        assertEquals("1.41", actualResultsTable.getRows().get(0).get("fte").getCurrentValue());
        assertEquals("2", actualResultsTable.getRows().get(0).get("headcount").getCurrentValue());

        assertEquals("2411 - ANSPAYAXW", actualResultsTable.getRows().get(1).get("title").getCurrentValue());
        assertEquals("0.50", actualResultsTable.getRows().get(1).get("fte").getCurrentValue());
        assertEquals("1", actualResultsTable.getRows().get(1).get("headcount").getCurrentValue());

        assertEquals("All Bands & Students", actualResultsTable.getRows().get(2).get("title").getCurrentValue());
        assertEquals("1.91", actualResultsTable.getRows().get(2).get("fte").getCurrentValue());
        assertEquals("3", actualResultsTable.getRows().get(2).get("headcount").getCurrentValue());
    }

    void saveStudentsWithBandCodes(){
        SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(schoolCollection);
        student1.setBandCode("0704");
        student1.setFte(new BigDecimal("0.4100"));
        studentRepository.save(student1);

        SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(schoolCollection);
        student2.setBandCode("2411");
        student2.setFte(new BigDecimal("0.5000"));
        studentRepository.save(student2);

        SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(schoolCollection);
        student3.setBandCode("0704");
        student3.setFte(new BigDecimal("1.0000"));
        studentRepository.save(student3);
    }

}
