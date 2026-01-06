package com.example.txprocessor.domain.exception;

public class ParentTransactionNotFoundException extends DomainException {
    private static final String MSG_FORMAT =
            "Parent Transaction with Id = '%s' not found for transaction with id = '%s'";
    public ParentTransactionNotFoundException(Long id, Long parentId) {
        super(
                DomainErrorType.parentTxNotFound,
                String.format(MSG_FORMAT, parentId, id),
                id,
                parentId
        );
    }
}
