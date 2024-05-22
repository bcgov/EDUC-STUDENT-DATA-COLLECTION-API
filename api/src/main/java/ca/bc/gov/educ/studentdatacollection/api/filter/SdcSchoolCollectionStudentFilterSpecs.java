package ca.bc.gov.educ.studentdatacollection.api.filter;

import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentPaginationEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.chrono.ChronoLocalDate;
import java.time.chrono.ChronoLocalDateTime;
import java.util.UUID;

@Service
@Slf4j
public class SdcSchoolCollectionStudentFilterSpecs extends BaseFilterSpecs<SdcSchoolCollectionStudentPaginationEntity> {

  public SdcSchoolCollectionStudentFilterSpecs(FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, ChronoLocalDate> dateFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, ChronoLocalDateTime<?>> dateTimeFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, Integer> integerFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, String> stringFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, Long> longFilterSpecifications, FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, UUID> uuidFilterSpecifications,FilterSpecifications<SdcSchoolCollectionStudentPaginationEntity, Boolean> booleanFilterSpecifications, Converters converters) {
    super(dateFilterSpecifications, dateTimeFilterSpecifications, integerFilterSpecifications, stringFilterSpecifications, longFilterSpecifications, uuidFilterSpecifications, booleanFilterSpecifications, converters);
  }

  public Specification<SdcSchoolCollectionStudentPaginationEntity> getSchoolIDSpecification(UUID schoolID) {
    return (root, query, builder) -> {
      Join<?, ?> schoolCollectionJoin = root.join("sdcSchoolCollection", JoinType.INNER);
      return builder.equal(schoolCollectionJoin.get("schoolID"), schoolID);
    };
  }

}
