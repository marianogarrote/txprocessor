package com.example.txprocessor.domain.exception;

import lombok.Getter;

public class DomainException extends RuntimeException {
    @Getter
    private final DomainErrorType type;

    public DomainException(DomainErrorType type, String message) {
        super(message);
        this.type = type;
    }
}