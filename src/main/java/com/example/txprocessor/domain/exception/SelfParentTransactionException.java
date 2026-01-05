package com.example.txprocessor.domain.exception;

import lombok.Getter;

public class SelfParentTransactionException extends DomainException {
    @Getter
    private final Long transactionId;
    public SelfParentTransactionException(Long transactionId) {
        super(
                DomainErrorType.selfParentTransactionException,
                "Transaction cannot be parent of itself [id = " + transactionId + "]"
        );
        this.transactionId = transactionId;
    }
}