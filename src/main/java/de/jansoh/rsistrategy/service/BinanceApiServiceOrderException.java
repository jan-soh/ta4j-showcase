package de.jansoh.rsistrategy.service;

public class BinanceApiServiceOrderException extends RuntimeException {
    public BinanceApiServiceOrderException(String message) {
        super(message);
    }

    public BinanceApiServiceOrderException(String message, Throwable cause) {
        super(message, cause);
    }
}
