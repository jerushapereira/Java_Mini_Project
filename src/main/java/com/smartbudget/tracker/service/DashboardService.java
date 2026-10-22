package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.CategoryTotal;
import com.smartbudget.tracker.dto.DashboardSummary;
import com.smartbudget.tracker.dto.MonthlySummary;
import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final TransactionRepository transactions;

    public DashboardService(TransactionRepository transactions) { this.transactions = transactions; }

    @Transactional(readOnly = true)
    public DashboardSummary summary(int month, int year) {
        YearMonth period = period(month, year);
        List<TransactionEntity> rows = transactions.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(period.atDay(1), period.atEndOfMonth());
        BigDecimal income = BigDecimal.ZERO.setScale(2), expenses = BigDecimal.ZERO.setScale(2);
        Map<String, BigDecimal> spending = new LinkedHashMap<>();
        for (TransactionEntity row : rows) {
            if (row.getType() == TransactionType.INCOME) income = income.add(row.getAmount());
            if (row.getType() == TransactionType.EXPENSE) {
                expenses = expenses.add(row.getAmount());
                String name = row.getCategory().getName();
                spending.merge(name, row.getAmount(), BigDecimal::add);
            }
        }
        List<CategoryTotal> categories = spending.entrySet().stream()
                .map(entry -> new CategoryTotal(entry.getKey(), entry.getValue().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(CategoryTotal::amount).reversed()).toList();
        return new DashboardSummary(month, year, income.setScale(2), expenses.setScale(2), income.subtract(expenses).setScale(2),
                rows.size(), categories, categories.isEmpty() ? null : categories.getFirst().category());
    }

    @Transactional(readOnly = true)
    public List<CategoryTotal> categorySpending(int month, int year) { return summary(month, year).categorySpending(); }

    @Transactional(readOnly = true)
    public List<MonthlySummary> monthlySummary() {
        YearMonth current = YearMonth.now();
        YearMonth start = current.minusMonths(11);
        List<TransactionEntity> rows = transactions.findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(start.atDay(1), current.atEndOfMonth());
        Map<YearMonth, Totals> totals = new LinkedHashMap<>();
        for (TransactionEntity row : rows) {
            YearMonth month = YearMonth.from(row.getTransactionDate());
            Totals accumulator = totals.computeIfAbsent(month, ignored -> new Totals());
            if (row.getType() == TransactionType.INCOME) accumulator.income = accumulator.income.add(row.getAmount());
            if (row.getType() == TransactionType.EXPENSE) accumulator.expenses = accumulator.expenses.add(row.getAmount());
        }
        List<MonthlySummary> result = new ArrayList<>();
        for (YearMonth month = current; !month.isBefore(start); month = month.minusMonths(1)) {
            Totals value = totals.getOrDefault(month, new Totals());
            result.add(new MonthlySummary(month.getYear(), month.getMonthValue(), value.income.setScale(2), value.expenses.setScale(2)));
        }
        return result;
    }

    private YearMonth period(int month, int year) {
        if (month < 1 || month > 12 || year < 2000 || year > 2200) throw new InvalidRequestException("Invalid month or year");
        return YearMonth.of(year, month);
    }

    private static final class Totals {
        private BigDecimal income = BigDecimal.ZERO.setScale(2);
        private BigDecimal expenses = BigDecimal.ZERO.setScale(2);
    }
}
