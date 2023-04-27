package ca.bc.gov.educ.studentdatacollection.api.controller.v1;

import ca.bc.gov.educ.studentdatacollection.api.endpoint.v1.SdcSchoolCollectionStudentEndpoint;
import ca.bc.gov.educ.studentdatacollection.api.exception.InvalidParameterException;
import ca.bc.gov.educ.studentdatacollection.api.exception.StudentDataCollectionAPIRuntimeException;
import ca.bc.gov.educ.studentdatacollection.api.filter.FilterOperation;
import ca.bc.gov.educ.studentdatacollection.api.filter.SdcSchoolCollectionStudentFilterSpecs;
import ca.bc.gov.educ.studentdatacollection.api.mappers.v1.SdcSchoolCollectionStudentMapper;
import ca.bc.gov.educ.studentdatacollection.api.model.v1.SdcSchoolCollectionStudentEntity;
import ca.bc.gov.educ.studentdatacollection.api.service.v1.SdcSchoolCollectionStudentService;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SdcSchoolCollectionStudent;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.SearchCriteria;
import ca.bc.gov.educ.studentdatacollection.api.struct.v1.ValueType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SdcSchoolCollectionStudentController implements SdcSchoolCollectionStudentEndpoint {

    private final SdcSchoolCollectionStudentFilterSpecs sdcSchoolCollectionStudentFilterSpecs;

    private final SdcSchoolCollectionStudentService sdcSchoolCollectionStudentService;

    private static final SdcSchoolCollectionStudentMapper mapper = SdcSchoolCollectionStudentMapper.mapper;

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public CompletableFuture<Page<SdcSchoolCollectionStudent>> findAll(Integer pageNumber, Integer pageSize, String sortCriteriaJson, String searchCriteriaListJson) {
        val objectMapper = new ObjectMapper();
        final List<Sort.Order> sorts = new ArrayList<>();
        Specification<SdcSchoolCollectionStudentEntity> sdcSchoolStudentSpecs = null;
        try {
            getSortCriteria(sortCriteriaJson, objectMapper, sorts);
            if (StringUtils.isNotBlank(searchCriteriaListJson)) {
                List<SearchCriteria> criteriaList = objectMapper.readValue(searchCriteriaListJson, new TypeReference<>() {
                });
                sdcSchoolStudentSpecs = getSdcSchoolCollectionStudentEntitySpecification(criteriaList);
            }
        } catch (JsonProcessingException e) {
            throw new StudentDataCollectionAPIRuntimeException(e.getMessage());
        }
        return this.sdcSchoolCollectionStudentService.findAll(sdcSchoolStudentSpecs, pageNumber, pageSize, sorts).thenApplyAsync(sdcSchoolStudentEntities -> sdcSchoolStudentEntities.map(mapper::toSdcSchoolStudent));
    }


    private void getSortCriteria(String sortCriteriaJson, ObjectMapper objectMapper, List<Sort.Order> sorts) throws JsonProcessingException {
        if (StringUtils.isNotBlank(sortCriteriaJson)) {
            Map<String, String> sortMap = objectMapper.readValue(sortCriteriaJson, new TypeReference<>() {
            });
            sortMap.forEach((k, v) -> {
                if ("ASC".equalsIgnoreCase(v)) {
                    sorts.add(new Sort.Order(Sort.Direction.ASC, k));
                } else {
                    sorts.add(new Sort.Order(Sort.Direction.DESC, k));
                }
            });
        }
    }

    private Specification<SdcSchoolCollectionStudentEntity> getSdcSchoolCollectionStudentEntitySpecification(List<SearchCriteria> criteriaList) {
        Specification<SdcSchoolCollectionStudentEntity> secureExchangeSpecs = null;
        if (!criteriaList.isEmpty()) {
            var i = 0;
            for (SearchCriteria criteria : criteriaList) {
                if (criteria.getKey() != null && criteria.getOperation() != null && criteria.getValueType() != null) {
                    Specification<SdcSchoolCollectionStudentEntity> typeSpecification = getTypeSpecification(criteria.getKey(), criteria.getOperation(), criteria.getValue(), criteria.getValueType());
                    if (i == 0) {
                        secureExchangeSpecs = Specification.where(typeSpecification);
                    } else {
                        assert secureExchangeSpecs != null;
                        secureExchangeSpecs = secureExchangeSpecs.and(typeSpecification);
                    }
                    i++;
                } else {
                    throw new InvalidParameterException("Search Criteria can not contain null values for key, value and operation type");
                }
            }
        }
        return secureExchangeSpecs;
    }

    private Specification<SdcSchoolCollectionStudentEntity> getTypeSpecification(String key, FilterOperation filterOperation, String value, ValueType valueType) {
        Specification<SdcSchoolCollectionStudentEntity> secureExchangeSpecs = null;
        switch (valueType) {
            case STRING:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getStringTypeSpecification(key, value, filterOperation);
                break;
            case DATE_TIME:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getDateTimeTypeSpecification(key, value, filterOperation);
                break;
            case LONG:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getLongTypeSpecification(key, value, filterOperation);
                break;
            case INTEGER:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getIntegerTypeSpecification(key, value, filterOperation);
                break;
            case DATE:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getDateTypeSpecification(key, value, filterOperation);
                break;
            case UUID:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getUUIDTypeSpecification(key, value, filterOperation);
                break;
            case BOOLEAN:
                secureExchangeSpecs = sdcSchoolCollectionStudentFilterSpecs.getBooleanTypeSpecification(key, value, filterOperation);
                break;
            default:
                break;
        }
        return secureExchangeSpecs;
    }
}
