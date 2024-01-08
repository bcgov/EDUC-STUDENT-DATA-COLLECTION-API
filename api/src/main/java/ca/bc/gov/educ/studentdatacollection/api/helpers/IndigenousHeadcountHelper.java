package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionEntity;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeader;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.HeadcountHeaderColumn;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndigenousHeadcountHeaderResult;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.IndigenousHeadcountResult;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.IntStream;

@Component
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class IndigenousHeadcountHelper extends HeadcountHelper<IndigenousHeadcountResult>{

    private static final String INDIGENOUS_LANGUAGE_TITLE = "Indigenous Language and Culture";
    private static final String INDIGENOUS_SUPPORT_TITLE = "Indigenous Support Services";
    private static final String OTHER_APPROVED_TITLE = "Other Approved Indigenous Programs";
    private static final String ANCESTRY_COUNT_TITLE = "Indigenous Ancestry Headcount";
    private static final String LIVING_ON_RESERVE_TITLE = "Ordinarily Living on Reserve Headcount";
    private static final String ELIGIBLE_TITLE = "Eligible";
    private static final String REPORTED_TITLE = "Reported";
    private static final String NOT_REPORTED_TITLE = "Not Reported";
    private static final List<String> HEADER_COLUMN_TITLES = List.of(ELIGIBLE_TITLE, REPORTED_TITLE, NOT_REPORTED_TITLE);
    public IndigenousHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository);
    }

    public void setComparisonValues(SdcSchoolCollectionEntity sdcSchoolCollectionEntity, List<HeadcountHeader> headcountHeaderList) {
        UUID previousCollectionID = getPreviousSeptemberCollectionID(sdcSchoolCollectionEntity);
        List<HeadcountHeader> previousHeadcountHeaderList = getHeaders(previousCollectionID);
        setComparisonValues(headcountHeaderList, previousHeadcountHeaderList);
    }

    public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID) {
        IndigenousHeadcountHeaderResult result = sdcSchoolCollectionStudentRepository.getIndigenousHeadersBySchoolId(sdcSchoolCollectionID);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();
        Arrays.asList(INDIGENOUS_LANGUAGE_TITLE, INDIGENOUS_SUPPORT_TITLE, OTHER_APPROVED_TITLE, ANCESTRY_COUNT_TITLE, LIVING_ON_RESERVE_TITLE).forEach(headerTitle -> {
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);
            switch (headerTitle) {
                case INDIGENOUS_LANGUAGE_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getEligIndigenousLanguage())).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(result.getReportedIndigenousLanguage())).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedIndigenousLanguage()))).build());
                }
                case INDIGENOUS_SUPPORT_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligIndigenousSupport()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedIndigenousSupport()).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedIndigenousSupport()))).build());
                }
                case OTHER_APPROVED_TITLE -> {
                    headcountHeader.setOrderedColumnTitles(HEADER_COLUMN_TITLES);
                    headcountHeader.getColumns().put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getEligOtherProgram()).build());
                    headcountHeader.getColumns().put(REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(result.getReportedOtherProgram()).build());
                    headcountHeader.getColumns().put(NOT_REPORTED_TITLE, HeadcountHeaderColumn.builder().currentValue(String.valueOf(Long.parseLong(result.getAllStudents()) - Long.parseLong(result.getReportedOtherProgram()))).build());
                }
                case ANCESTRY_COUNT_TITLE -> headcountHeader.setHeadCountValue(HeadcountHeaderColumn.builder().currentValue(result.getStudentsWithIndigenousAncestry()).build());
                case LIVING_ON_RESERVE_TITLE -> headcountHeader.setHeadCountValue(HeadcountHeaderColumn.builder().currentValue(result.getStudentsWithFundingCode20()).build());
                default -> {
                    log.error("Unexpected header title.  This cannot happen::" + headerTitle);
                    throw new StudentDataCollectionAPIRuntimeException("Unexpected header title.  This cannot happen::" + headerTitle);
                }
            }
            headcountHeaderList.add(headcountHeader);
        });
        return headcountHeaderList;
    }

    @Override
    public void setComparisonValues(List<HeadcountHeader> headcountHeaderList, List<HeadcountHeader> previousHeadcountHeaderList) {
        IntStream.range(0, headcountHeaderList.size())
                .forEach(i -> {
                    HeadcountHeader currentHeader = headcountHeaderList.get(i);
                    HeadcountHeader previousHeader = previousHeadcountHeaderList.get(i);

                    currentHeader.getColumns().forEach((columnName, currentColumn) -> {
                        HeadcountHeaderColumn previousColumn = previousHeader.getColumns().get(columnName);
                        currentColumn.setComparisonValue(previousColumn.getCurrentValue());
                    });

                    if(currentHeader.getHeadCountValue() != null && previousHeader.getHeadCountValue() != null) {
                        currentHeader.getHeadCountValue().setComparisonValue(previousHeader.getHeadCountValue().getCurrentValue());
                    }
                });
    }
}
