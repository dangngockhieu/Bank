package vn.bank.khieu.utils;

import org.springframework.data.jpa.domain.Specification;

public class GenericSpecification {

    public static <T> Specification<T> equal(String field, Object value) {
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    public static <T> Specification<T> like(String field, String value) {
        return (root, query, cb) -> cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }

    public static <T> Specification<T> greaterThanOrEqual(String field, Number value) {
        return (root, query, cb) -> cb.ge(root.get(field), value);
    }

    public static <T> Specification<T> lessThanOrEqual(String field, Number value) {
        return (root, query, cb) -> cb.le(root.get(field), value);
    }
}
