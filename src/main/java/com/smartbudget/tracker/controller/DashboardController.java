package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.CategoryTotal;
import com.smartbudget.tracker.dto.DashboardSummary;
import com.smartbudget.tracker.dto.MonthlySummary;
import com.smartbudget.tracker.dto.BudgetResponse;
import com.smartbudget.tracker.dto.TransactionResponse;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.service.BudgetService;
import com.smartbudget.tracker.service.DashboardService;
import com.smartbudget.tracker.service.TransactionService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboard;
    private final BudgetService budgets;
    private final TransactionService transactions;

    public DashboardController(DashboardService dashboard, BudgetService budgets, TransactionService transactions) {
        this.dashboard = dashboard;
        this.budgets = budgets;
        this.transactions = transactions;
    }

    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        int[] period = period(month, year);
        return dashboard.summary(period[0], period[1]);
    }

    @GetMapping("/category-spending")
    public List<CategoryTotal> categorySpending(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        int[] period = period(month, year);
        return dashboard.categorySpending(period[0], period[1]);
    }

    @GetMapping("/monthly-summary")
    public List<MonthlySummary> monthlySummary() { return dashboard.monthlySummary(); }

    @GetMapping("/budget-status")
    public List<BudgetResponse> budgetStatus(@RequestParam(required = false) Integer month, @RequestParam(required = false) Integer year) {
        int[] period = period(month, year);
        return budgets.list(period[0], period[1]);
    }

    @GetMapping("/recent-transactions")
    public List<TransactionResponse> recentTransactions() { return transactions.recent(); }

    private int[] period(Integer month, Integer year) {
        if ((month == null) != (year == null)) throw new InvalidRequestException("month and year must be provided together");
        LocalDate today = LocalDate.now();
        return new int[] { month == null ? today.getMonthValue() : month, year == null ? today.getYear() : year };
    }
}
