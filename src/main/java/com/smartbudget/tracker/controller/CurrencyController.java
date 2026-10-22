package com.smartbudget.tracker.controller;

import com.smartbudget.tracker.dto.CurrencyConversionResponse;
import com.smartbudget.tracker.dto.CurrencyRatesResponse;
import com.smartbudget.tracker.exception.InvalidRequestException;
import com.smartbudget.tracker.service.CurrencyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/currency")
public class CurrencyController {
    private final CurrencyService currency;
    public CurrencyController(CurrencyService currency) { this.currency = currency; }

    @GetMapping("/rates")
    public CurrencyRatesResponse rates(@RequestParam(defaultValue = "INR") String base,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return currency.getRates(base, date);
    }

    @GetMapping("/convert")
    public CurrencyConversionResponse convert(@RequestParam BigDecimal amount,
                                              @RequestParam(defaultValue = "INR") String from,
                                              @RequestParam(defaultValue = "USD") String to,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return currency.convert(amount, from, to, date);
    }
}
