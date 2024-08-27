package ca.bc.gov.educ.studentdatacollection.api.constants.v1;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum SpecialEducationCategoryTitles {
    A_PHYSICALLY_DEPENDENT("A - Physically Dependent"),
    B_DEAFBLIND("B - Deafblind"),
    C_MODERATE_TO_PROFOUND_INTELLECTUAL_DISABILITY("C - Moderate to Profound Intellectual Disability"),
    D_PHYSICAL_DISABILITY_OR_CHRONIC_HEALTH_IMPAIRMENT("D - Physical Disability or Chronic Health Impairment"),
    E_VISUAL_IMPAIRMENT("E - Visual Impairment"),
    F_DEAF_OR_HARD_OF_HEARING("F - Deaf or Hard of Hearing"),
    G_AUTISM_SPECTRUM_DISORDER("G - Autism Spectrum Disorder"),
    H_INTENSIVE_BEHAVIOUR_INTERVENTION_SERIOUS_MENTAL_ILLNESS("H - Intensive Behaviour Interventions or Serious Mental Illness"),
    K_MILD_INTELLECTUAL_DISABILITY("K - Mild Intellectual Disability"),
    P_GIFTED("P - Gifted"),
    Q_LEARNING_DISABILITY("Q - Learning Disability"),
    R_MODERATE_BEHAVIOUR_SUPPORT_MENTAL_ILLNESS("R - Moderate Behaviour Support/Mental Illness")
    ;

    @Getter
    private final String title;
    SpecialEducationCategoryTitles(String title) {
        this.title = title;
    }

    public static Optional<SpecialEducationCategoryTitles> findByValue(String value) {
        return Arrays.stream(values()).filter(e -> Arrays.asList(e.title).contains(value)).findFirst();
    }

    public static List<String> getTitles() {
        return Arrays.stream(SpecialEducationCategoryTitles.values()).map(SpecialEducationCategoryTitles::getTitle).collect(Collectors.toList());
    }
}
