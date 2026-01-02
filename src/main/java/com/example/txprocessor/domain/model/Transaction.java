package com.example.txprocessor.domain.model;

import com.example.txprocessor.domain.exception.CircularTransactionCreationException;
import com.example.txprocessor.domain.exception.TransactionCreationException;

import java.math.BigDecimal;
import java.util.Objects;

public record Transaction(Long id, BigDecimal amount, TransactionType type, Long parentId) {

    public Transaction {
        if (id == null) {
            throw new TransactionCreationException("id");
        }
        if (amount == null) {
            throw new TransactionCreationException("amount");
        }
        if (type == null) {
            throw new TransactionCreationException("type");
        }
        if (Objects.equals(id, parentId)) {
            throw new CircularTransactionCreationException(id);
        }
    }
}