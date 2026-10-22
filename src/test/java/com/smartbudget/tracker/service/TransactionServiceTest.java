package com.smartbudget.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartbudget.tracker.dto.TransactionRequest;
import com.smartbudget.tracker.dto.TransactionResponse;
import com.smartbudget.tracker.entity.CategoryEntity;
import com.smartbudget.tracker.entity.TransactionEntity;
import com.smartbudget.tracker.entity.TransactionType;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.repository.ActivityRepository;
import com.smartbudget.tracker.repository.CategoryRepository;
import com.smartbudget.tracker.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TransactionServiceTest {
    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final TransactionService service = new TransactionService(transactions, categories, activities);

    @Test
    void createsExpenseAndPersistsActivity() {
        CategoryEntity food = new CategoryEntity("Food");
        when(categories.findByNameIgnoreCase("Food")).thenReturn(Optional.of(food));
        when(transactions.save(any(TransactionEntity.class))).thenAnswer(call -> call.getArgument(0));
        TransactionRequest request = new TransactionRequest(TransactionType.EXPENSE, new BigDecimal("12.30"),
                "Food", "Lunch", LocalDate.of(2026, 10, 22), "Card", null);

        TransactionResponse response = service.create(request);

        assertEquals(new BigDecimal("12.30"), response.amount());
        assertEquals("Food", response.category());
        assertEquals(LocalDate.of(2026, 10, 22), response.transactionDate());
        verify(activities).save(any());
    }

    @Test
    void rejectsNonPositiveTransactionAmount() {
        TransactionRequest request = new TransactionRequest(TransactionType.INCOME, BigDecimal.ZERO,
                null, "Pay", LocalDate.of(2026, 10, 22), null, "Salary");

        assertThrows(InvalidRequestException.class, () -> service.create(request));
    }
}
