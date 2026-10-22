package com.smartbudget.tracker.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FrankfurterRate(LocalDate date, String base, String quote, BigDecimal rate) {}
