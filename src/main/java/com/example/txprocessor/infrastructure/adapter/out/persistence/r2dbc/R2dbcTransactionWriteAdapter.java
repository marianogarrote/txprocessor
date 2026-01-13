package com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.model.TransactionWriteEntity;
import com.example.txprocessor.infrastructure.adapter.out.persistence.r2dbc.repository.TransactionWriteRepository;
import com.example.txprocessor.infrastructure.adapter.out.persistence.repository.TransactionWriter;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class R2dbcTransactionWriteAdapter implements TransactionWriter {
    private final TransactionWriteRepository repository;

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return repository.save(toEntity(transaction))
                .map(it -> transaction);
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
