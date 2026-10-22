package com.smartbudget.tracker.dto;

import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record TransactionResponse(Long id, TransactionType type, BigDecimal amount, String category,
                                  String description, LocalDate transactionDate, String paymentMode,
                                  String incomeSource, Instant createdAt, Instant updatedAt) {
    public static TransactionResponse from(TransactionEntity entity) {
        return new TransactionResponse(entity.getId(), entity.getType(), entity.getAmount(),
                entity.getCategory() == null ? null : entity.getCategory().getName(), entity.getDescription(),
                entity.getTransactionDate(), entity.getPaymentMode(), entity.getIncomeSource(),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }
}
