package com.smartbudget.tracker.repository;

import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class TransactionSpecifications {
    private TransactionSpecifications() {}

    public static Specification<TransactionEntity> filter(TransactionType type, String category, LocalDate from,
                                                            LocalDate to, String query, String paymentMode,
                                                            String incomeSource) {
        return (root, criteria, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            var categoryJoin = root.join("category", jakarta.persistence.criteria.JoinType.LEFT);
            if (type != null) predicates.add(builder.equal(root.get("type"), type));
            if (category != null && !category.isBlank()) predicates.add(builder.equal(builder.lower(categoryJoin.get("name")), category.toLowerCase()));
            if (from != null) predicates.add(builder.greaterThanOrEqualTo(root.get("transactionDate"), from));
            if (to != null) predicates.add(builder.lessThanOrEqualTo(root.get("transactionDate"), to));
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.toLowerCase() + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("description")), pattern),
                        builder.like(builder.lower(root.get("incomeSource")), pattern),
                        builder.like(builder.lower(categoryJoin.get("name")), pattern)));
            }
            if (paymentMode != null && !paymentMode.isBlank()) predicates.add(builder.equal(builder.lower(root.get("paymentMode")), paymentMode.toLowerCase()));
            if (incomeSource != null && !incomeSource.isBlank()) predicates.add(builder.equal(builder.lower(root.get("incomeSource")), incomeSource.toLowerCase()));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
