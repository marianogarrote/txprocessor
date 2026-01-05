package com.example.txprocessor.domain.exception;

import lombok.Getter;

public class ParentTransactionNotFoundException extends DomainException {
    private static final String MSG_FORMAT =
            "Parent Transaction with Id = '%s' not found for transaction with id = '%s'";
    @Getter
    private final Long id;
    @Getter
    private final Long parentId;

    public ParentTransactionNotFoundException(Long id, Long parentId) {
        super(
                DomainErrorType.parentTxNotFound,
                String.format(MSG_FORMAT, parentId, id)
        );
        this.id = id;
        this.parentId = parentId;
    }
}
