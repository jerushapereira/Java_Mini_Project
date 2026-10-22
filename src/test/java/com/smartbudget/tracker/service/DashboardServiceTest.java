package com.smartbudget.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.smartbudget.tracker.dto.DashboardSummary;
import com.smartbudget.tracker.entity.CategoryEntity;
import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {
    @Test
    void summarizesPersistedIncomeAndExpenseAmounts() {
        TransactionRepository transactions = mock(TransactionRepository.class);
        CategoryEntity food = new CategoryEntity("Food");
        when(transactions.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)))
                .thenReturn(List.of(
                        new TransactionEntity(TransactionType.INCOME, new BigDecimal("1000.00"), null,
                                "Pay", LocalDate.of(2026, 10, 10), null, "Salary"),
                        new TransactionEntity(TransactionType.EXPENSE, new BigDecimal("250.25"), food,
                                "Groceries", LocalDate.of(2026, 10, 9), "Card", null)));

        DashboardSummary summary = new DashboardService(transactions).summary(10, 2026);

        assertEquals(new BigDecimal("1000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("250.25"), summary.totalExpenses());
        assertEquals(new BigDecimal("749.75"), summary.balance());
        assertEquals(2, summary.transactionCount());
        assertEquals("Food", summary.highestSpendingCategory());
    }
}
