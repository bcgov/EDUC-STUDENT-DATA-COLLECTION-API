package ca.bc.gov.educ.studentdatacollection.api.utils;

import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.BandHeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountChildNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountNode;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports.HeadcountReportNode;
import ca.bc.gov.educ.studentdatacollection.api.util.TextNormalizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;

class TextNormalizerTest {
    static class SchoolNameList extends ArrayList<String> {}
    static class BandNameMap extends HashMap<String, String> {}
    static class SchoolNodeSet extends HashSet<HeadcountChildNode> {}

    static class SchoolBatch {
        String[] names;
        SchoolBatch(String... names) { this.names = names; }
    }

    static class SchoolReportMap extends HashMap<String, HeadcountReportNode> {}

    static class ReportBatch {
        HeadcountReportNode[] nodes;
        ReportBatch(HeadcountReportNode... nodes) { this.nodes = nodes; }
    }

    static class SelfRefNode {
        String name;
        SelfRefNode self;
        SelfRefNode(String name) { this.name = name; this.self = this; }
    }

    private static String toNFD(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD);
    }

    private static void assertIsNFC(String value) {
        assertThat(Normalizer.isNormalized(value, Normalizer.Form.NFC))
                .as("Expected NFC-normalized string but was: %s", value)
                .isTrue();
    }

    @Test
    void normalizeObject_nullInput_returnsNull() {
        assertThat((Object) TextNormalizer.normalizeObject(null)).isNull();
    }

    @Test
    void normalizeObject_nullStringField_remainsNull() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(null);
        TextNormalizer.normalizeObject(node);
        assertThat(node.getSchoolMincodeAndName()).isNull();
    }

    @Test
    void normalizeObject_emptyStringField_remainsEmpty() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName("");
        TextNormalizer.normalizeObject(node);
        assertThat(node.getSchoolMincodeAndName()).isEmpty();
    }

    @Test
    void normalizeObject_returnsSameObjectReference() {
        HeadcountReportNode node = new HeadcountReportNode();
        assertThat(TextNormalizer.normalizeObject(node)).isSameAs(node);
    }

    @Test
    void normalizeObject_schoolNameWithModifierLetterApostrophe_preservedAsNfc() {
        String name = "08502060 - Gwa\u02BCsala-\u02BCNakwaxda\u02BCxw School";
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(name);

        TextNormalizer.normalizeObject(node);

        assertThat(node.getSchoolMincodeAndName()).isEqualTo(name);
        assertIsNFC(node.getSchoolMincodeAndName());
    }

    @Test
    void normalizeObject_districtNameInNfd_recomposedToNfc() {
        String nfdDistrict = toNFD("085 - Sto:ló District");
        assertThat(Normalizer.isNormalized(nfdDistrict, Normalizer.Form.NFC)).isFalse();

        HeadcountReportNode node = new HeadcountReportNode();
        node.setDistrictNumberAndName(nfdDistrict);

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.getDistrictNumberAndName());
    }

    @Test
    void normalizeObject_allTombstoneFields_normalised() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setDistrictNumberAndName(toNFD("048 - Sq\u0313wme\u0301ylem District"));  // q̓ + é in NFD
        node.setSchoolMincodeAndName(toNFD("04802060 - E\u0301cole Francophone"));
        node.setCollectionNameAndYear("February 2026 Collection");
        node.setReportGeneratedDate("Report Generated: 2026-02-17");

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.getDistrictNumberAndName());
        assertIsNFC(node.getSchoolMincodeAndName());

        assertThat(node.getCollectionNameAndYear()).isEqualTo("February 2026 Collection");
    }


    @Test
    void normalizeObject_headcountNode_nestedReportNodeNormalised() {
        HeadcountReportNode reportNode = new HeadcountReportNode();
        reportNode.setDistrictNumberAndName(toNFD("K\u0313o\u0301smos Indigenous District"));

        HeadcountNode mainNode = new HeadcountNode();
        mainNode.setReport(reportNode);

        TextNormalizer.normalizeObject(mainNode);

        assertIsNFC(mainNode.getReport().getDistrictNumberAndName());
    }


    @Test
    void normalizeObject_headcountChildNode_typeOfProgramNormalised() {
        HeadcountChildNode node = new HeadcountChildNode(
                toNFD("q\u0313ay\u0301 First Nations Band"), "true", "00");

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.getTypeOfProgram());
    }

    @Test
    void normalizeObject_bandHeadcountChildNode_inheritedTypeOfProgramNormalised() {
        BandHeadcountChildNode node = new BandHeadcountChildNode(
                toNFD("Gwa\u0301sala-Na\u0301kwaxda\u0301xw Nations"), "false", "10");

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.getTypeOfProgram());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
            "acute on e, e\u0301, \u00E9",
            "cedilla on c, c\u0327, \u00E7",
            "acute on E, E\u0301, \u00C9",
    })
    void normalizeObject_frenchAccents_recomposed(String description, String nfdInput, String expectedNfc) {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(nfdInput);

        TextNormalizer.normalizeObject(node);

        assertThat(node.getSchoolMincodeAndName()).as(description).isEqualTo(expectedNfc);
        assertIsNFC(node.getSchoolMincodeAndName());
    }

    @Test
    void normalizeObject_frenchSchoolName_recomposed() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(toNFD("03901020 - É\u0301cole Secondaire Fran\u00E7ais"));

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.getSchoolMincodeAndName());
    }

    @Test
    void normalizeObject_cyclicReference_doesNotThrow() {
        HeadcountNode a = new HeadcountNode();
        HeadcountReportNode report = new HeadcountReportNode();
        report.setDistrictNumberAndName(toNFD("Sto\u0301:lo\u0301 District"));
        a.setReport(report);

        TextNormalizer.normalizeObject(a);

        assertIsNFC(a.getReport().getDistrictNumberAndName());
    }

    @Test
    void normalizeObject_stringArrayField_elementsNormalized() {
        SchoolBatch batch = new SchoolBatch(
                toNFD("04802060 - E\u0301cole Francophone"),
                toNFD("Sto:lo\u0301 Community School"),
                "Already NFC School");

        TextNormalizer.normalizeObject(batch);

        for (String name : batch.names) {
            assertIsNFC(name);
        }
    }

    @Test
    void normalizeObject_customListOfSchoolNames_elementsNormalized() {
        SchoolNameList list = new SchoolNameList();
        list.add(toNFD("Gwa\u0301sala School"));
        list.add(toNFD("E\u0301cole Secondaire"));
        list.add("Already NFC");

        TextNormalizer.normalizeObject(list);

        list.forEach(name -> assertIsNFC(name));
    }

    @Test
    void normalizeObject_customSetOfChildNodes_pojoElementsNormalized() {
        HeadcountChildNode node = new HeadcountChildNode(
                toNFD("q\u0313ay\u0301 First Nations"), "true", "00");
        SchoolNodeSet set = new SchoolNodeSet();
        set.add(node);

        TextNormalizer.normalizeObject(set);

        assertIsNFC(node.getTypeOfProgram());
    }

    @Test
    void normalizeObject_customMapOfBandNames_valuesNormalized() {
        BandNameMap map = new BandNameMap();
        map.put("0610", toNFD("Ktunaxa Nation - q\u0313ay\u0301"));
        map.put("0620", "Already NFC Band");

        TextNormalizer.normalizeObject(map);

        map.values().forEach(v -> assertIsNFC(v));
    }

    @Test
    void normalizeObject_mapWithNfdKey_keyReplacedWithNfc() {
        BandNameMap map = new BandNameMap();
        String nfdKey = "e\u0301cole";          // 'e' + U+0301 combining acute (NFD)
        map.put(nfdKey, "Some School");

        TextNormalizer.normalizeObject(map);

        assertThat(map).containsKey("\u00E9cole");   // U+00E9 = precomposed é (NFC)
        assertThat(map).doesNotContainKey(nfdKey);
    }

    @Test
    void normalizeObject_mapWithPojoValues_pojoFieldsNormalized() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(toNFD("E\u0301cole Secondaire"));
        SchoolReportMap map = new SchoolReportMap();
        map.put("school1", node);

        TextNormalizer.normalizeObject(map);

        assertIsNFC(map.get("school1").getSchoolMincodeAndName());
    }

    @Test
    void normalizeObject_arrayOfPojos_elementsNormalized() {
        HeadcountReportNode node = new HeadcountReportNode();
        node.setSchoolMincodeAndName(toNFD("E\u0301cole Secondaire"));
        ReportBatch batch = new ReportBatch(node);

        TextNormalizer.normalizeObject(batch);

        assertIsNFC(batch.nodes[0].getSchoolMincodeAndName());
    }

    @Test
    void normalizeObject_trueCyclicReference_doesNotThrow() {
        SelfRefNode node = new SelfRefNode(toNFD("E\u0301cole"));

        TextNormalizer.normalizeObject(node);

        assertIsNFC(node.name);
    }
}
