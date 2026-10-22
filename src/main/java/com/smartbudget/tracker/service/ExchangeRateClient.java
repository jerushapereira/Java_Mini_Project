package com.smartbudget.tracker.service;

import java.time.LocalDate;
import java.util.List;

public interface ExchangeRateClient {
    List<FrankfurterRate> rates(String base, LocalDate date);
    FrankfurterRate rate(String base, String quote, LocalDate date);
}
