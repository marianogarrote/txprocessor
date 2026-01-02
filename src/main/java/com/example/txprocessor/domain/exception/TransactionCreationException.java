package com.example.txprocessor.domain.exception;

public class TransactionCreationException extends DomainException {
    private static final String MSG_FORMAT = "Transaction creation: Invalid value for field '%s' = '%s'";

    public TransactionCreationException(String field, Object value) {
        super(
                DomainErrorType.invalidFieldValueDuringTxCreation,
                String.format(MSG_FORMAT, field, value)
        );
    }

    public TransactionCreationException(String field) {
        this(field, null);
    }
}