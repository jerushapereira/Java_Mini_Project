package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.BudgetRequest;
import com.smartbudget.tracker.dto.BudgetResponse;
import com.smartbudget.tracker.entity.ActivityEntity;
import com.smartbudget.tracker.entity.BudgetEntity;
import com.smartbudget.tracker.entity.CategoryEntity;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.exception.ResourceNotFoundException;
import com.smartbudget.tracker.repository.ActivityRepository;
import com.smartbudget.tracker.repository.BudgetRepository;
import com.smartbudget.tracker.repository.CategoryRepository;
import com.smartbudget.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BudgetService {
    private final BudgetRepository budgets;
    private final CategoryRepository categories;
    private final TransactionRepository transactions;
    private final ActivityRepository activities;

    public BudgetService(BudgetRepository budgets, CategoryRepository categories, TransactionRepository transactions, ActivityRepository activities) {
        this.budgets = budgets;
        this.categories = categories;
        this.transactions = transactions;
        this.activities = activities;
    }

    @Transactional(readOnly = true)
    public List<BudgetResponse> list(int month, int year) {
        validatePeriod(month, year);
        List<BudgetEntity> items = budgets.findByMonthAndYearOrderByCategoryName(month, year);
        LocalDate from = LocalDate.of(year, month, 1), to = from.withDayOfMonth(from.lengthOfMonth());
        Map<String, BigDecimal> spending = transactions.sumByCategoryAndPeriod(TransactionType.EXPENSE, from, to).stream()
                .collect(Collectors.toMap(row -> (String) row[0], row -> (BigDecimal) row[1]));
        return items.stream().map(budget -> BudgetResponse.from(budget,
                spending.getOrDefault(budget.getCategory().getName(), BigDecimal.ZERO.setScale(2)))).toList();
    }

    @Transactional(readOnly = true)
    public BudgetResponse get(long id) {
        BudgetEntity budget = find(id);
        LocalDate from = LocalDate.of(budget.getYear(), budget.getMonth(), 1), to = from.withDayOfMonth(from.lengthOfMonth());
        return BudgetResponse.from(budget, zeroIfNull(transactions.sumForCategoryAndPeriod(TransactionType.EXPENSE, budget.getCategory().getName(), from, to)));
    }

    @Transactional
    public BudgetResponse create(BudgetRequest request) {
        validatePeriod(request.month(), request.year());
        CategoryEntity category = category(request.category());
        BudgetEntity saved = budgets.saveAndFlush(new BudgetEntity(category, request.monthlyLimit(), request.month(), request.year()));
        activities.save(new ActivityEntity("BUDGET_CREATED", "Budget created for " + category.getName(), "BUDGET", saved.getId()));
        return get(saved.getId());
    }

    @Transactional
    public BudgetResponse update(long id, BudgetRequest request) {
        validatePeriod(request.month(), request.year());
        BudgetEntity budget = find(id);
        CategoryEntity category = category(request.category());
        budget.replace(category, request.monthlyLimit(), request.month(), request.year());
        activities.save(new ActivityEntity("BUDGET_UPDATED", "Budget " + id + " updated", "BUDGET", id));
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        BudgetEntity budget = find(id);
        activities.save(new ActivityEntity("BUDGET_DELETED", "Budget " + id + " deleted", "BUDGET", id));
        budgets.delete(budget);
    }

    private CategoryEntity category(String name) {
        if (name == null || name.isBlank()) throw new InvalidRequestException("category is required");
        return categories.findByNameIgnoreCase(name.trim()).orElseThrow(() -> new InvalidRequestException("Unsupported category"));
    }

    private BudgetEntity find(long id) { return budgets.findById(id).orElseThrow(() -> new ResourceNotFoundException("Budget not found")); }

    private void validatePeriod(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) throw new InvalidRequestException("Invalid budget month or year");
    }

    private BigDecimal zeroIfNull(BigDecimal value) { return value == null ? BigDecimal.ZERO.setScale(2) : value; }
}
