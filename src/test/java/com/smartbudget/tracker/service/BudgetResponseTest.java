package com.smartbudget.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.smartbudget.tracker.dto.BudgetResponse;
import com.smartbudget.tracker.entity.BudgetEntity;
import com.smartbudget.tracker.entity.CategoryEntity;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BudgetResponseTest {
    @Test
    void flagsWarningAtEightyPercentAndExceededAtFullSpend() {
        BudgetEntity budget = new BudgetEntity(new CategoryEntity("Food"), new BigDecimal("100.00"), 10, 2026);

        BudgetResponse warning = BudgetResponse.from(budget, new BigDecimal("80.00"));
        BudgetResponse exceeded = BudgetResponse.from(budget, new BigDecimal("100.00"));

        assertEquals(true, warning.warning());
        assertEquals(false, warning.exceeded());
        assertEquals(new BigDecimal("20.00"), warning.remaining());
        assertEquals(false, exceeded.warning());
        assertEquals(true, exceeded.exceeded());
        assertEquals(new BigDecimal("0.00"), exceeded.remaining());
    }
}
