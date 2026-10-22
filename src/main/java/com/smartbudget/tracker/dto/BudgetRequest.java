package com.smartbudget.tracker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record BudgetRequest(
        @NotBlank @Size(max = 80) String category,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal monthlyLimit,
        @Min(1) @Max(12) int month,
        @Min(2000) @Max(2200) int year) {}
