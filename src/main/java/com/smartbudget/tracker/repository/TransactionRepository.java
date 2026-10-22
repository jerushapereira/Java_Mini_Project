package com.smartbudget.tracker.repository;

import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<TransactionEntity, Long>, JpaSpecificationExecutor<TransactionEntity> {
    List<TransactionEntity> findByTransactionDateBetweenOrderByTransactionDateDescIdDesc(LocalDate from, LocalDate to);
    List<TransactionEntity> findTop10ByOrderByTransactionDateDescIdDesc();

    @Query("select coalesce(sum(t.amount), 0) from TransactionEntity t where t.type = :type and t.category.name = :category and t.transactionDate between :from and :to")
    BigDecimal sumForCategoryAndPeriod(@Param("type") TransactionType type, @Param("category") String category,
                                       @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select c.name, coalesce(sum(t.amount), 0) from TransactionEntity t join t.category c where t.type = :type and t.transactionDate between :from and :to group by c.name")
    List<Object[]> sumByCategoryAndPeriod(@Param("type") TransactionType type,
                                          @Param("from") LocalDate from, @Param("to") LocalDate to);
}
