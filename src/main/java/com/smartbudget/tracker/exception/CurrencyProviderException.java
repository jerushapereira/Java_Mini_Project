package com.smartbudget.tracker.exception;

public class CurrencyProviderException extends RuntimeException {
    private final int status;

    public CurrencyProviderException(String message, int status) { super(message); this.status = status; }
    public CurrencyProviderException(String message, int status, Throwable cause) { super(message, cause); this.status = status; }
    public int getStatus() { return status; }
}
