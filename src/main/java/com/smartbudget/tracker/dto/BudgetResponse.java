package com.smartbudget.tracker.dto;

import com.smartbudget.tracker.entity.BudgetEntity;
import java.math.BigDecimal;
import java.time.Instant;

public record BudgetResponse(Long id, String category, BigDecimal monthlyLimit, int month, int year,
                             BigDecimal spent, BigDecimal remaining, BigDecimal percentUsed,
                             boolean warning, boolean exceeded, Instant createdAt, Instant updatedAt) {
    public static BudgetResponse from(BudgetEntity budget, BigDecimal spent) {
        BigDecimal limit = budget.getMonthlyLimit();
        BigDecimal remaining = limit.subtract(spent).setScale(2);
        BigDecimal percent = spent.multiply(BigDecimal.valueOf(100))
                .divide(limit, 2, java.math.RoundingMode.HALF_UP);
        boolean exceeded = spent.compareTo(limit) >= 0;
        boolean warning = !exceeded && percent.compareTo(BigDecimal.valueOf(80)) >= 0;
        return new BudgetResponse(budget.getId(), budget.getCategory().getName(), limit, budget.getMonth(),
                budget.getYear(), spent.setScale(2), remaining, percent, warning, exceeded,
                budget.getCreatedAt(), budget.getUpdatedAt());
    }
}
