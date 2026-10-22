package com.smartbudget.tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummary(int month, int year, BigDecimal totalIncome, BigDecimal totalExpenses,
                               BigDecimal balance, long transactionCount, List<CategoryTotal> categorySpending,
                               String highestSpendingCategory) {}
