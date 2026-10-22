package com.smartbudget.tracker.repository;

import com.smartbudget.tracker.entity.BudgetEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BudgetRepository extends JpaRepository<BudgetEntity, Long> {
    List<BudgetEntity> findByMonthAndYearOrderByCategoryName(int month, int year);
}
