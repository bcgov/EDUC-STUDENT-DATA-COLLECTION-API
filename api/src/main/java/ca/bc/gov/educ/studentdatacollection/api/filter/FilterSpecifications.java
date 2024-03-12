package ca.bc.gov.educ.studentdatacollection.api.filter;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.function.Function;

@Service
public class FilterSpecifications<E, T extends Comparable<T>> {

    private EnumMap<FilterOperation, Function<FilterCriteria<T>, Specification<E>>> map;

    public FilterSpecifications() {
        initSpecifications();
    }

    public Function<FilterCriteria<T>, Specification<E>> getSpecification(FilterOperation operation) {
        return map.get(operation);
    }

    @PostConstruct
    public void initSpecifications() {

        map = new EnumMap<>(FilterOperation.class);

        // Equal
        map.put(FilterOperation.EQUAL, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.equal(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            } else if(filterCriteria.getConvertedSingleValue() == null) {
                return criteriaBuilder.isNull(root.get(filterCriteria.getFieldName()));
            }
            return criteriaBuilder.equal(root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.NOT_EQUAL, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.notEqual(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            } else if(filterCriteria.getConvertedSingleValue() == null) {
                return criteriaBuilder.isNotNull(root.get(filterCriteria.getFieldName()));
            }
            return criteriaBuilder.notEqual(root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.GREATER_THAN, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.greaterThan(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            }
            return criteriaBuilder.greaterThan(root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.GREATER_THAN_OR_EQUAL_TO, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.greaterThanOrEqualTo(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            }
            return criteriaBuilder.greaterThanOrEqualTo(
                    root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.LESS_THAN, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.lessThan(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            }
            return criteriaBuilder.lessThan(root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.LESS_THAN_OR_EQUAL_TO, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.lessThanOrEqualTo(root.join(splits[0]).get(splits[1]), filterCriteria.getConvertedSingleValue());
            }
           return criteriaBuilder.lessThanOrEqualTo(root.get(filterCriteria.getFieldName()), filterCriteria.getConvertedSingleValue());
        });

        map.put(FilterOperation.IN, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return root.join(splits[0]).get(splits[1]).in(filterCriteria.getConvertedValues());
            }
            return root.get(filterCriteria.getFieldName()).in(filterCriteria.getConvertedValues());
        });

        map.put(FilterOperation.NOT_IN, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.or(criteriaBuilder.not(root.join(splits[0], JoinType.LEFT).get(splits[1]).in(filterCriteria.getConvertedValues())), criteriaBuilder.isEmpty(root.get(splits[0])));
            }
            return criteriaBuilder.or(criteriaBuilder.not(root.get(filterCriteria.getFieldName()).in(filterCriteria.getConvertedValues())), criteriaBuilder.isNull(root.get(filterCriteria.getFieldName())));
        });

        map.put(FilterOperation.NONE_IN,
            filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
                if (filterCriteria.getFieldName().contains(".")) {
                    String[] splits = filterCriteria.getFieldName().split("\\.");
                    Subquery<String> subquery = criteriaQuery.subquery(String.class);
                    Root<E> subRoot = subquery.from(root.getModel().getJavaType());

                    Join<E, ?> childJoin = subRoot.join(splits[0], JoinType.LEFT);
                    Predicate childPredicate = childJoin.get(splits[1]).in(filterCriteria.getConvertedValues());

                    subquery.select(subRoot.get("sdcSchoolCollectionStudentID"))
                            .where(childPredicate);
                    return criteriaBuilder.not(root.get("sdcSchoolCollectionStudentID").in(subquery));
                } else {
                    return criteriaBuilder.or(criteriaBuilder.not(root.get(filterCriteria.getFieldName()).in(filterCriteria.getConvertedValues())), criteriaBuilder.isNull(root.get(filterCriteria.getFieldName())));
                }
            });

        map.put(FilterOperation.BETWEEN,
                filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(
                        root.get(filterCriteria.getFieldName()), filterCriteria.getMinValue(),
                        filterCriteria.getMaxValue()));

        map.put(FilterOperation.CONTAINS, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                .like(root.get(filterCriteria.getFieldName()), "%" + filterCriteria.getConvertedSingleValue() + "%"));

        map.put(FilterOperation.CONTAINS_IGNORE_CASE, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder
                .like(criteriaBuilder.lower(root.get(filterCriteria.getFieldName())), "%" + filterCriteria.getConvertedSingleValue().toString().toLowerCase() + "%"));

        map.put(FilterOperation.IN_LEFT_JOIN, filterCriteria -> (root, criteriaQuery, criteriaBuilder) -> {
            criteriaQuery.distinct(true);
            if (filterCriteria.getFieldName().contains(".")) {
                String[] splits = filterCriteria.getFieldName().split("\\.");
                return criteriaBuilder.or(root.join(splits[0], JoinType.LEFT).get(splits[1]).in(filterCriteria.getConvertedValues()), criteriaBuilder.isNotEmpty(root.get(splits[0])));
            }
            return root.get(filterCriteria.getFieldName()).in(filterCriteria.getConvertedValues());
        });

    }
}
