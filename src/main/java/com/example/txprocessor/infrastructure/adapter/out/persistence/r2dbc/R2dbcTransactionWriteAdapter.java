package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionWriteEntity;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository.TransactionWriteRepository;
import com.example.txprocessor.infrastructure.adapter.out.persistence.repository.TransactionWriter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Slf4j
public class R2dbcTransactionWriteAdapter implements TransactionWriter {
    private final TransactionWriteRepository repository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.upsert(
                transaction.id(),
                transaction.amount(),
                transaction.type(),
                transaction.hasParent()? transaction.parent().id() : null
                )
                .thenReturn(transaction)
                .doOnError(error -> log.error("Error saving tx: {}", transaction));
    }

    private TransactionWriteEntity toEntity(Transaction tx) {
        return new TransactionWriteEntity(
                tx.id(),
                tx.amount(),
                tx.type(),
                tx.parent() != null? tx.parent().id() : null
        );
    }
}
