package com.smartbudget.tracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartbudget.tracker.dto.CurrencyConversionResponse;
import com.smartbudget.tracker.exception.CurrencyProviderException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurrencyServiceTest {
    private final ExchangeRateClient client = mock(ExchangeRateClient.class);
    private final CurrencyService service = new CurrencyService(client, Duration.ofHours(6));

    @Test
    void convertsUsingProviderDecimalAndCachesRate() {
        when(client.rate("INR", "USD", null)).thenReturn(new FrankfurterRate(
                LocalDate.of(2026, 10, 21), "INR", "USD", new BigDecimal("0.012345")));

        CurrencyConversionResponse first = service.convert(new BigDecimal("100.25"), "inr", "usd", null);
        CurrencyConversionResponse second = service.convert(new BigDecimal("2.00"), "INR", "USD", null);

        assertEquals(new BigDecimal("1.24"), first.convertedAmount());
        assertEquals(LocalDate.of(2026, 10, 21), first.rateDate());
        assertEquals(false, first.cached());
        assertEquals(new BigDecimal("0.02"), second.convertedAmount());
        assertEquals(true, second.cached());
        verify(client).rate("INR", "USD", null);
    }

    @Test
    void doesNotInventRateWhenProviderFailsWithoutCache() {
        when(client.rates("INR", null)).thenThrow(new CurrencyProviderException("unavailable", 503));

        assertThrows(CurrencyProviderException.class, () -> service.getRates("INR", null));
        verify(client).rates("INR", null);
        verify(client, never()).rate("INR", "USD", null);
    }

    @Test
    void rejectsMalformedProviderRows() {
        when(client.rates("INR", null)).thenReturn(List.of(
                new FrankfurterRate(LocalDate.now(), "EUR", "USD", new BigDecimal("1.2"))));

        assertThrows(CurrencyProviderException.class, () -> service.getRates("INR", null));
    }
}
