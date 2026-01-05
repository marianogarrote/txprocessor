package com.example.txprocessor.domain.exception;

public enum DomainErrorType {
    circularTransactionGraphException,
    selfParentTransactionException,
    invalidFieldValueDuringTxCreation,
    parentTxNotFound
}