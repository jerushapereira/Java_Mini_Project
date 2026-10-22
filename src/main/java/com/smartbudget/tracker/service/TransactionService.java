package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.PageResponse;
import com.smartbudget.tracker.dto.TransactionRequest;
import com.smartbudget.tracker.dto.TransactionResponse;
import com.smartbudget.tracker.entity.ActivityEntity;
import com.smartbudget.tracker.entity.CategoryEntity;
import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.exception.ResourceNotFoundException;
import com.smartbudget.tracker.repository.ActivityRepository;
import com.smartbudget.tracker.repository.CategoryRepository;
import com.smartbudget.tracker.repository.TransactionRepository;
import com.smartbudget.tracker.repository.TransactionSpecifications;
import java.time.LocalDate;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final CategoryRepository categories;
    private final ActivityRepository activities;

    public TransactionService(TransactionRepository transactions, CategoryRepository categories, ActivityRepository activities) {
        this.transactions = transactions;
        this.categories = categories;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> search(TransactionType type, String category, LocalDate from, LocalDate to,
                                                     String query, String paymentMode, String incomeSource,
                                                     int page, int pageSize) {
        if (page < 1 || pageSize < 1 || pageSize > 100) throw new InvalidRequestException("page must be positive and pageSize must be between 1 and 100");
        if (from != null && to != null && from.isAfter(to)) throw new InvalidRequestException("from date must not be after to date");
        Page<TransactionResponse> result = transactions.findAll(
                TransactionSpecifications.filter(type, category, from, to, query, paymentMode, incomeSource),
                PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"))))
                .map(TransactionResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public TransactionResponse get(long id) {
        return TransactionResponse.from(find(id));
    }

    @Transactional
    public TransactionResponse create(TransactionRequest request) {
        Prepared prepared = prepare(request);
        TransactionEntity saved = transactions.save(new TransactionEntity(request.type(), request.amount(), prepared.category(),
                clean(request.description()), request.transactionDate(), prepared.paymentMode(), prepared.incomeSource()));
        activities.save(new ActivityEntity(request.type() == TransactionType.INCOME ? "INCOME_ADDED" : "EXPENSE_ADDED",
                "Transaction " + saved.getId() + " recorded", "TRANSACTION", saved.getId()));
        return TransactionResponse.from(saved);
    }

    @Transactional
    public TransactionResponse update(long id, TransactionRequest request) {
        TransactionEntity entity = find(id);
        Prepared prepared = prepare(request);
        entity.replace(request.type(), request.amount(), prepared.category(), clean(request.description()),
                request.transactionDate(), prepared.paymentMode(), prepared.incomeSource());
        activities.save(new ActivityEntity("TRANSACTION_UPDATED", "Transaction " + id + " updated", "TRANSACTION", id));
        return TransactionResponse.from(entity);
    }

    @Transactional
    public void delete(long id) {
        TransactionEntity entity = find(id);
        activities.save(new ActivityEntity("TRANSACTION_DELETED", "Transaction " + id + " deleted", "TRANSACTION", id));
        transactions.delete(entity);
    }

    @Transactional(readOnly = true)
    public java.util.List<TransactionResponse> recent() {
        return transactions.findTop10ByOrderByTransactionDateDescIdDesc().stream().map(TransactionResponse::from).toList();
    }

    private TransactionEntity find(long id) {
        return transactions.findById(id).orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
    }

    private Prepared prepare(TransactionRequest request) {
        if (request.amount() == null || request.amount().signum() <= 0) throw new InvalidRequestException("amount must be greater than zero");
        if (request.transactionDate() == null) throw new InvalidRequestException("transactionDate is required");
        if (request.type() == TransactionType.EXPENSE) {
            if (request.category() == null || request.category().isBlank()) throw new InvalidRequestException("category is required for an expense");
            if (request.paymentMode() == null || request.paymentMode().isBlank()) throw new InvalidRequestException("paymentMode is required for an expense");
            CategoryEntity category = categories.findByNameIgnoreCase(request.category().trim())
                    .orElseThrow(() -> new InvalidRequestException("Unsupported category"));
            return new Prepared(category, request.paymentMode().trim(), null);
        }
        if (request.type() == TransactionType.INCOME) {
            if (request.incomeSource() == null || request.incomeSource().isBlank()) throw new InvalidRequestException("incomeSource is required for income");
            return new Prepared(null, null, request.incomeSource().trim());
        }
        throw new InvalidRequestException("type must be INCOME or EXPENSE");
    }

    private String clean(String value) { return value == null ? "" : value.trim(); }
    private record Prepared(CategoryEntity category, String paymentMode, String incomeSource) {}
}
