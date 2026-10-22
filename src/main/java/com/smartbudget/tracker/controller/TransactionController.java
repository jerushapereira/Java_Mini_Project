package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.PageResponse;
import com.smartbudget.tracker.dto.TransactionRequest;
import com.smartbudget.tracker.dto.TransactionResponse;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.service.TransactionService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactions;
    public TransactionController(TransactionService transactions) { this.transactions = transactions; }

    @GetMapping
    public PageResponse<TransactionResponse> list(
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) String paymentMode,
            @RequestParam(required = false) String incomeSource,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        if ((month == null) != (year == null)) throw new InvalidRequestException("month and year must be provided together");
        if (month != null) {
            if (month < 1 || month > 12 || year < 2000 || year > 2200) throw new InvalidRequestException("Invalid month or year");
            LocalDate first = YearMonth.of(year, month).atDay(1), last = YearMonth.of(year, month).atEndOfMonth();
            from = from == null || first.isAfter(from) ? first : from;
            to = to == null || last.isBefore(to) ? last : to;
        }
        return transactions.search(type, category, from, to, query, paymentMode, incomeSource, page, pageSize);
    }

    @GetMapping("/{id}")
    public TransactionResponse get(@PathVariable long id) { return transactions.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse create(@Valid @RequestBody TransactionRequest request) { return transactions.create(request); }

    @PutMapping("/{id}")
    public TransactionResponse update(@PathVariable long id, @Valid @RequestBody TransactionRequest request) { return transactions.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { transactions.delete(id); }
}
