package com.smartbudget.tracker.service;

import com.smartbudget.tracker.dto.CurrencyConversionResponse;
import com.smartbudget.tracker.dto.CurrencyRatesResponse;
import com.smartbudget.tracker.dto.ExchangeRateResponse;
import com.smartbudget.tracker.exception.CurrencyProviderException;
import com.smartbudget.tracker.exception.InvalidRequestException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurrencyService {
    private static final String SOURCE = "Frankfurter";
    private final ExchangeRateClient client;
    private final Duration ttl;
    private final ConcurrentMap<String, Cached<CurrencyRatesResponse>> rateCache = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Cached<FrankfurterRate>> pairCache = new ConcurrentHashMap<>();

    public CurrencyService(ExchangeRateClient client, @Value("${currency.cache-ttl}") Duration ttl) {
        this.client = client;
        this.ttl = ttl;
    }

    public CurrencyRatesResponse getRates(String rawBase, LocalDate date) {
        String base = currencyCode(rawBase);
        String key = (date == null ? "latest" : date.toString()) + ":" + base;
        Cached<CurrencyRatesResponse> cached = rateCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(java.time.Instant.now())) return markCached(cached.value(), false);
        try {
            List<ExchangeRateResponse> rates = client.rates(base, date).stream().map(row -> validateResponse(row, base, null)).toList();
            CurrencyRatesResponse response = new CurrencyRatesResponse(base, SOURCE, rates, false, false);
            rateCache.put(key, new Cached<>(response, java.time.Instant.now().plus(ttl)));
            return response;
        } catch (RuntimeException exception) {
            if (cached != null) return markCached(cached.value(), true);
            throw exception;
        }
    }

    public CurrencyConversionResponse convert(BigDecimal amount, String rawFrom, String rawTo, LocalDate date) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 8) throw new InvalidRequestException("amount must be positive with at most 8 decimal places");
        String from = currencyCode(rawFrom), to = currencyCode(rawTo);
        if (from.equals(to)) return new CurrencyConversionResponse(amount, from, to, BigDecimal.ONE.setScale(8),
                amount.setScale(2, RoundingMode.HALF_UP), date == null ? LocalDate.now() : date,
                "Identity conversion", false, false);
        String key = (date == null ? "latest" : date.toString()) + ":" + from + ":" + to;
        Cached<FrankfurterRate> cached = pairCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(java.time.Instant.now())) return conversion(amount, from, to, cached.value(), true, false);
        try {
            FrankfurterRate rate = validateProviderRate(client.rate(from, to, date), from, to);
            pairCache.put(key, new Cached<>(rate, java.time.Instant.now().plus(ttl)));
            return conversion(amount, from, to, rate, false, false);
        } catch (RuntimeException exception) {
            if (cached != null) return conversion(amount, from, to, cached.value(), true, true);
            if (exception instanceof CurrencyProviderException) throw exception;
            throw new CurrencyProviderException("Frankfurter is temporarily unavailable", 503, exception);
        }
    }

    private CurrencyConversionResponse conversion(BigDecimal amount, String from, String to, FrankfurterRate rate, boolean cached, boolean stale) {
        return new CurrencyConversionResponse(amount, from, to, rate.rate(), amount.multiply(rate.rate()).setScale(2, RoundingMode.HALF_UP),
                rate.date(), SOURCE, cached, stale);
    }

    private CurrencyRatesResponse markCached(CurrencyRatesResponse response, boolean stale) {
        return new CurrencyRatesResponse(response.base(), response.source(), response.rates(), true, stale);
    }

    private ExchangeRateResponse validateResponse(FrankfurterRate row, String base, String quote) {
        if (row == null || row.date() == null || !base.equals(row.base()) || (quote != null && !quote.equals(row.quote()))
                || row.rate() == null || row.rate().signum() <= 0 || (row.quote() != null && !row.quote().matches("[A-Z]{3}"))
                || (quote != null && (row.quote() == null || !row.quote().matches("[A-Z]{3}")))) {
            throw new CurrencyProviderException("Frankfurter returned malformed rate data", 503);
        }
        return new ExchangeRateResponse(row.date(), row.base(), row.quote(), row.rate());
    }

    private FrankfurterRate validateProviderRate(FrankfurterRate row, String base, String quote) {
        validateResponse(row, base, quote);
        return row;
    }

    private String currencyCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) throw new InvalidRequestException("currency must be a three-letter code");
        return code;
    }

    private record Cached<T>(T value, java.time.Instant expiresAt) {}
}
