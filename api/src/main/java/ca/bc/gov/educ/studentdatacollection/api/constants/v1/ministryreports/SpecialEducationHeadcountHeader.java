package ca.bc.gov.educ.studentdatacollection.api.constants.v1.ministryreports;

import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Getter
public enum SpecialEducationHeadcountHeader {

    SCHOOL("School Name"),
    A("A - Physically Dependent"),
    B("B - Deafblind"),
    C("C - Moderate to Profound Intellectual Disability"),
    D("D - Physical Disability or Chronic Health Impairment"),
    E("E - Visual Impairment"),
    F("F - Deaf or Hard of Hearing"),
    G("G - Autism Spectrum Disorder"),
    H("H - Intensive Behaviour Interventions or Serious Mental Illness"),
    K("K - Mild Intellectual Disability"),
    P("P - Gifted"),
    Q("Q - Learning Disability"),
    R("R - Moderate Behaviour Support/Mental Illness"),
    TOTAL("Total");

    private final String code;
    SpecialEducationHeadcountHeader(String code) { this.code = code; }

    public static HashMap<String, Integer> generateSpedCatCountMap() {
        return Stream.of(SpecialEducationHeadcountHeader.values())
                .filter(header -> !header.equals(SCHOOL) && !header.equals(TOTAL))
                .collect(Collectors.toMap(
                        header -> header.name(),
                        header -> 0,
                        (oldValue, newValue) -> oldValue,
                        HashMap::new
                ));
    }

    public static List<String> getCatTitles() {
        return Stream.of(SpecialEducationHeadcountHeader.values())
                .filter(header -> !header.equals(SCHOOL) && !header.equals(TOTAL))
                .map(SpecialEducationHeadcountHeader::getCode)
                .collect(Collectors.toList());
    }
}
