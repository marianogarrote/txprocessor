package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;

import java.math.BigDecimal;

public final class TransactionFactory {
    public static Transaction sample() {
        return new Transaction(1L, BigDecimal.valueOf(100), TransactionType.cars, null);
    }
}