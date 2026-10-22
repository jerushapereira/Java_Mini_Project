package com.smartbudget.tracker.config;

import com.smartbudget.tracker.exception.CurrencyProviderException;
import com.smartbudget.tracker.service.ExchangeRateClient;
import com.smartbudget.tracker.service.FrankfurterRate;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClientException;

@Component
public class FrankfurterHttpClient implements ExchangeRateClient {
    private final RestClient client;

    public FrankfurterHttpClient(@Value("${currency.api.base-url}") String baseUrl,
                                 @Value("${currency.connect-timeout}") Duration connectTimeout,
                                 @Value("${currency.request-timeout}") Duration requestTimeout) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(requestTimeout);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
    }

    @Override
    public List<FrankfurterRate> rates(String base, LocalDate date) {
        try {
            FrankfurterRate[] rows = client.get().uri(builder -> {
                var uri = builder.path("/rates").queryParam("base", base);
                if (date != null) uri.queryParam("date", date);
                return uri.build();
            }).retrieve().body(FrankfurterRate[].class);
            if (rows == null) throw new CurrencyProviderException("Frankfurter returned an empty response", 503);
            return Arrays.asList(rows);
        } catch (RestClientResponseException exception) {
            throw providerError(exception);
        } catch (RestClientException exception) {
            throw new CurrencyProviderException("Frankfurter is unavailable or returned malformed data", 503, exception);
        }
    }

    @Override
    public FrankfurterRate rate(String base, String quote, LocalDate date) {
        try {
            FrankfurterRate result = client.get().uri(builder -> {
                var uri = builder.path("/rate/{base}/{quote}");
                if (date != null) uri.queryParam("date", date);
                return uri.build(base.toLowerCase(), quote.toLowerCase());
            }).retrieve().body(FrankfurterRate.class);
            if (result == null) throw new CurrencyProviderException("Frankfurter returned an empty response", 503);
            return result;
        } catch (RestClientResponseException exception) {
            throw providerError(exception);
        } catch (RestClientException exception) {
            throw new CurrencyProviderException("Frankfurter is unavailable or returned malformed data", 503, exception);
        }
    }

    private CurrencyProviderException providerError(RestClientResponseException exception) {
        HttpStatusCode status = exception.getStatusCode();
        if (status.value() == 404 || status.value() == 422 || status.value() == 400) {
            return new CurrencyProviderException("Currency or rate is not supported by Frankfurter", 400, exception);
        }
        return new CurrencyProviderException("Frankfurter is temporarily unavailable", 503, exception);
    }
}
