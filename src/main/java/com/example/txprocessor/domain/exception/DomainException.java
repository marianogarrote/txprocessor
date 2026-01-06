package com.example.txprocessor.domain.exception;

import lombok.Getter;

public class DomainException extends RuntimeException {
    @Getter
    private final DomainErrorType type;
    @Getter
    private final Long rootTxId;
    @Getter
    private final Long parentTxId;

    public DomainException(DomainErrorType type, String message, Long rootTxId, Long parentTxId) {
        super(message);
        this.type = type;
        this.rootTxId = rootTxId;
        this.parentTxId = parentTxId;
    }
}