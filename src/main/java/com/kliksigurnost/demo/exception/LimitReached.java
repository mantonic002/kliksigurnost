package com.kliksigurnost.demo.exception;

public class LimitReached extends RuntimeException {
    public LimitReached(String message) {
        super(message);
    }
}
