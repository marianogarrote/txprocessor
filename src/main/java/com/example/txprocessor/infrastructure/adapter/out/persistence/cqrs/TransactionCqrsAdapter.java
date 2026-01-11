package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@AllArgsConstructor
public class TransactionCqrsAdapter implements TransactionPort {
    private final TransactionCommand command;
    private final TransactionQuery query;

    @Override
    public Mono<Transaction> findById(Long id) {
        return query.findById(id);
    }

    @Override
    public Flux<Long> findIdsByType(TransactionType type) {
        return query.findIdsByType(type);
    }

    @Override
    public Flux<Transaction> streamSubgraph(Long rootTxId) {
        return query.streamSubgraph(rootTxId);
    }

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return command.save(transaction);
    }
}