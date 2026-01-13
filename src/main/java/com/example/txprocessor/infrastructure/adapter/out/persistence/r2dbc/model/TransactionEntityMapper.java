package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model;

import com.example.txprocessor.domain.model.Transaction;

public final class TransactionEntityMapper {

    public static TransactionWriteEntity toWriteEntity(Transaction tx) {
        return new TransactionWriteEntity(
                tx.id(),
                tx.amount(),
                tx.type(),
                tx.parent() != null ? tx.parent().id() : null
        );
    }

    public static Transaction toDomain(TransactionReadEntity e, Transaction parent) {
        return new Transaction(
                e.getId(),
                e.getAmount(),
                e.getType(),
                parent
        );
    }
}