package ca.bc.gov.educ.studentdatacollection.api.helpers;

import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcDistrictCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionRepository;
import ca.bc.gov.educ.studentdatacollection.api.repository.v1.SdcSchoolCollectionStudentRepository;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.headcounts.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class RefugeeHeadcountHelper extends HeadcountHelper<RefugeeHeadcountResult> {

    // Header Titles
    private static final String REFUGEE_TITLE = "Newcomer Refugees";
    private static final String ELIGIBLE_TITLE = "Eligible";
    private static final String REPORTED_TITLE = "Reported";

    public RefugeeHeadcountHelper(SdcSchoolCollectionRepository sdcSchoolCollectionRepository, SdcSchoolCollectionStudentRepository sdcSchoolCollectionStudentRepository, SdcDistrictCollectionRepository sdcDistrictCollectionRepository) {
        super(sdcSchoolCollectionRepository, sdcSchoolCollectionStudentRepository, sdcDistrictCollectionRepository);
    }


    public List<HeadcountHeader> getHeaders(UUID sdcSchoolCollectionID, boolean isDistrict) {
        RefugeeHeadcountHeaderResult result = isDistrict
                ? sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySdcDistrictCollectionId(sdcSchoolCollectionID)
                : sdcSchoolCollectionStudentRepository.getRefugeeHeadersBySchoolId(sdcSchoolCollectionID);

        List<String> refugeeColumnTitles = List.of(ELIGIBLE_TITLE, REPORTED_TITLE);
        List<HeadcountHeader> headcountHeaderList = new ArrayList<>();

        List.of(REFUGEE_TITLE).forEach(headerTitle -> {
            HeadcountHeader headcountHeader = new HeadcountHeader();
            headcountHeader.setColumns(new HashMap<>());
            headcountHeader.setTitle(headerTitle);

            if (StringUtils.equals(headerTitle, REFUGEE_TITLE)) {
                headcountHeader.setOrderedColumnTitles(refugeeColumnTitles);
                headcountHeader.getColumns()
                        .put(ELIGIBLE_TITLE, HeadcountHeaderColumn.builder()
                                .currentValue(String.valueOf(result.getEligibleStudents())).build());
                headcountHeader.getColumns()
                        .put(REPORTED_TITLE, HeadcountHeaderColumn.builder()
                                .currentValue(String.valueOf(result.getReportedStudents())).build());
            } else { log.warn("Unexpected case headerTitle.  This should not have happened."); }

            headcountHeaderList.add(headcountHeader);
        });
        return headcountHeaderList;
    }
}
