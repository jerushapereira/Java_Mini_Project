package com.smartbudget.tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CurrencyConversionResponse(BigDecimal amount, String from, String to, BigDecimal rate,
                                         BigDecimal convertedAmount, LocalDate rateDate, String source,
                                         boolean cached, boolean stale) {}
