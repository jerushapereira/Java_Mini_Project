package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.BudgetRequest;
import com.smartbudget.tracker.dto.BudgetResponse;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.service.BudgetService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
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
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgets;
    public BudgetController(BudgetService budgets) { this.budgets = budgets; }

    @GetMapping
    public List<BudgetResponse> list(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        if ((month == null) != (year == null)) throw new InvalidRequestException("month and year must be provided together");
        LocalDate today = LocalDate.now();
        return budgets.list(month == null ? today.getMonthValue() : month, year == null ? today.getYear() : year);
    }

    @GetMapping("/{id}")
    public BudgetResponse get(@PathVariable long id) { return budgets.get(id); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetResponse create(@Valid @RequestBody BudgetRequest request) { return budgets.create(request); }

    @PutMapping("/{id}")
    public BudgetResponse update(@PathVariable long id, @Valid @RequestBody BudgetRequest request) { return budgets.update(id, request); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id) { budgets.delete(id); }
}
