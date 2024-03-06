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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

        bandCodeRepository.saveAll(createBandCodeEntityList());

        CollectionEntity collectionEntity = createMockCollectionEntity();
        collectionRepository.save(collectionEntity);
        UUID schoolID = UUID.randomUUID();
        schoolCollection = createMockSdcSchoolCollectionEntity(collectionEntity, schoolID, UUID.randomUUID());
        schoolCollectionRepository.save(schoolCollection);

        SdcSchoolCollectionStudentEntity student1 = createMockSchoolStudentEntity(schoolCollection);
        student1.setBandCode("0704");
        student1.setFte(BigDecimal.valueOf(.41));
        studentRepository.save(student1);

        SdcSchoolCollectionStudentEntity student2 = createMockSchoolStudentEntity(schoolCollection);
        student2.setBandCode("2411");
        student2.setFte(BigDecimal.valueOf(.5));
        studentRepository.save(student2);

        SdcSchoolCollectionStudentEntity student3 = createMockSchoolStudentEntity(schoolCollection);
        student3.setBandCode("0704");
        student3.setFte(BigDecimal.valueOf(1));
        studentRepository.save(student3);

        helper = new BandResidenceHeadcountHelper(schoolCollectionRepository, studentRepository, codeTableService);

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

        List<String> expectedTitles = Arrays.asList("0704 - KANAKA BAR", "2411 - ANSPAYAXW");
        helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());
        assertEquals(expectedTitles, new ArrayList<>(helper.getBandRowTitles().values()));
    }

    @Test
    void testGetHeadcountHeaders_ShouldReturnListOfHeaders() {

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

        helper.getBandTitles(schoolCollection.getSdcSchoolCollectionID());

        List<BandResidenceHeadcountResult> result = studentRepository.getBandResidenceHeadcountsBySchoolId(schoolCollection.getSdcSchoolCollectionID());
        HeadcountResultsTable actualResultsTable = helper.convertBandHeadcountResults(result);

        List<String> headers = Arrays.asList("Indigenous Language and Culture", "Headcount", "FTE");
        List<Map<String, String>> expectedRows = new ArrayList<>();
        Map<String, String> row1 = new HashMap<>(Map.of("title", "0704 - KANAKA BAR", "fte", "1.41", "headcount", "2"));
        expectedRows.add(row1);
        Map<String, String> row2 = new HashMap<>(Map.of("title", "2411 - ANSPAYAXW", "fte", "0.50", "headcount", "1"));
        expectedRows.add(row2);
        Map<String, String> totalRow = new HashMap<>(Map.of("title", "All Bands & Students", "fte", "1.91", "headcount", "3"));
        expectedRows.add(totalRow);
        HeadcountResultsTable expectedResultsTable = new HeadcountResultsTable(headers, expectedRows);

        assertEquals(expectedResultsTable, actualResultsTable);
    }

    public List<BandCodeEntity> createBandCodeEntityList(){
        List<BandCodeEntity> bandCodeList = new ArrayList<>();
        bandCodeList.add(BandCodeEntity.builder().bandCode("0704").description("KANAKA BAR")
                .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(1).label("KANAKA BAR").createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build());
        bandCodeList.add(BandCodeEntity.builder().bandCode("2411").description("ANSPAYAXW")
                .effectiveDate(LocalDateTime.now()).expiryDate(LocalDateTime.MAX).displayOrder(2).label("ANSPAYAXW").createDate(LocalDateTime.now())
                .updateDate(LocalDateTime.now()).createUser("TEST").updateUser("TEST").build());

        return bandCodeList;
    }

}
