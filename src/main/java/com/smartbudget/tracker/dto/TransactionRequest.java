package com.smartbudget.tracker.dto;

import com.smartbudget.tracker.entity.TransactionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotNull TransactionType type,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Size(max = 80) String category,
        @Size(max = 300) String description,
        @NotNull LocalDate transactionDate,
        @Size(max = 80) String paymentMode,
        @Size(max = 120) String incomeSource) {}
