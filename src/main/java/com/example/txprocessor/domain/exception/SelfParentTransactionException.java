package com.example.txprocessor.domain.exception;

public class SelfParentTransactionException extends DomainException {
    public SelfParentTransactionException(Long transactionId) {
        super(
                DomainErrorType.selfParentTransactionException,
                "Transaction cannot be parent of itself [id = " + transactionId + "]",
                transactionId,
                transactionId
        );
    }
}