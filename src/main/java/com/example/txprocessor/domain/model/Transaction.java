package com.example.txprocessor.domain.model;

import com.example.txprocessor.domain.exception.CircularTransactionCreationException;
import com.example.txprocessor.domain.exception.TransactionCreationException;

import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(Long id, BigDecimal amount, TransactionType type, Transaction parent) {

    public Transaction {
        if (id == null) {
            throw new TransactionCreationException(null, "id");
        }
        if (amount == null) {
            throw new TransactionCreationException(id, "amount");
        }
        if (type == null) {
            throw new TransactionCreationException(id, "type");
        }
        if (parent != null) {
            validateNoGraphCycles(parent);
        }
    }

    private void validateNoGraphCycles(Transaction parent) {
        Transaction current = parent;
        while (current != null) {
            if (Objects.equals(this.id, current.id)) {
                throw new CircularTransactionCreationException(this.id);
            }
            current = current.parent();
        }
    }

    public boolean hasParent() {
        return parent != null;
    }
}