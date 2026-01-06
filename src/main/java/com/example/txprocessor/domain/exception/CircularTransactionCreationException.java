package com.example.txprocessor.domain.exception;

public class CircularTransactionCreationException extends DomainException {
    private static final String MSG_FORMAT = "Transaction contains circular relationship [id = %s]";

    public CircularTransactionCreationException(Long id) {
        super(
                DomainErrorType.circularTransactionGraphException,
                String.format(MSG_FORMAT, id),
                id,
                null
        );
    }
}