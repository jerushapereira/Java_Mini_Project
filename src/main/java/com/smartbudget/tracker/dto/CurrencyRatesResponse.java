package com.smartbudget.tracker.dto;

import java.util.List;

public record CurrencyRatesResponse(String base, String source, List<ExchangeRateResponse> rates,
                                    boolean cached, boolean stale) {}
