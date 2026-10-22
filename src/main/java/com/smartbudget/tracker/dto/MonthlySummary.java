package com.smartbudget.tracker.dto;

import java.math.BigDecimal;

public record MonthlySummary(int year, int month, BigDecimal income, BigDecimal expenses) {}
