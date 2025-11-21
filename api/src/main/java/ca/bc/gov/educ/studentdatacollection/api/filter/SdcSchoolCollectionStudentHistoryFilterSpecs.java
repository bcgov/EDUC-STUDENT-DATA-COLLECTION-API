package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentHistoryPaginationEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SdcSchoolCollectionStudentHistoryFilterSpecs extends BaseFilterSpecs<SdcSchoolCollectionStudentHistoryPaginationEntity>{
    public SdcSchoolCollectionStudentHistoryFilterSpecs(FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, Integer> integerFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, String> stringFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, Long> longFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, UUID> uuidFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentHistoryPaginationEntity, Boolean> booleanFilterSpecifications, Converters converters) {
        super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
    }
}
