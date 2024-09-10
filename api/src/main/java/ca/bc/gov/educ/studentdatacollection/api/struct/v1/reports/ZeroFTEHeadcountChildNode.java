package ca.bc.gov.educ.studentdatacollection.api.struct.v1.reports;

import ca.bc.gov.educ.studentdatacollection.api.constants.v1.SchoolGradeCodes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.util.Optional;

@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Slf4j
public class ZeroFTEHeadcountChildNode extends HeadcountChildNode implements Serializable {
    private static final long serialVersionUID = 6118916290604876032L;

    private String valueGradeKF = "0";

    private String valueGrade01 = "0";

    private String valueGrade02 = "0";

    private String valueGrade03 = "0";

    private String valueGrade04 = "0";

    private String valueGrade05 = "0";

    private String valueGrade06 = "0";

    private String valueGrade07 = "0";

    private String valueGradeEU = "0";

    private String valueGrade08 = "0";

    private String valueGrade09 = "0";

    private String valueGrade10 = "0";

    private String valueGrade11 = "0";

    private String valueGrade12 = "0";

    private String valueGradeSU = "0";

    private String valueGradeGA = "0";

    private String valueGradeHS = "0";

    private String valueTotal = "0";

    public ZeroFTEHeadcountChildNode(String typeOfProgram, String isHeading, String sequence) {
        super(typeOfProgram, isHeading, sequence);
    }

    public void setValueForGrade(String gradeCode, String value) {
        Optional<SchoolGradeCodes> schoolGradeCode = SchoolGradeCodes.findByValue(gradeCode);
        if (schoolGradeCode.isPresent()) {
            switch (schoolGradeCode.get()) {
                case KINDFULL -> setValueGradeKF(value);
                case GRADE01 -> setValueGrade01(value);
                case GRADE02 -> setValueGrade02(value);
                case GRADE03 -> setValueGrade03(value);
                case GRADE04 -> setValueGrade04(value);
                case GRADE05 -> setValueGrade05(value);
                case GRADE06 -> setValueGrade06(value);
                case GRADE07 -> setValueGrade07(value);
                case ELEMUNGR -> setValueGradeEU(value);
                case GRADE08 -> setValueGrade08(value);
                case GRADE09 -> setValueGrade09(value);
                case GRADE10 -> setValueGrade10(value);
                case GRADE11 -> setValueGrade11(value);
                case GRADE12 -> setValueGrade12(value);
                case SECONDARY_UNGRADED -> setValueGradeSU(value);
                case GRADUATED_ADULT -> setValueGradeGA(value);
                case HOMESCHOOL -> setValueGradeHS(value);
                default -> {
                }
            }
        }
    }

}