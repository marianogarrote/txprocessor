package com.example.txprocessor.domain.exception;

public class CircularTransactionCreationException extends DomainException {
    private static final String MSG_FORMAT = "Transaction cannot be parent of itself [id = %s]";

    public CircularTransactionCreationException(Long id) {
        super(DomainErrorType.circularTransactionCreation, String.format(MSG_FORMAT, id));
    }
}