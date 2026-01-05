package com.example.txprocessor.application.port.out;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionPort {
    Mono<Transaction> findById(Long id);
    Flux<Long> findIdsByType(TransactionType type);
    Mono<Transaction> save(Transaction transaction);
    Flux<Transaction> streamSubgraph(Long rootTxId);
}