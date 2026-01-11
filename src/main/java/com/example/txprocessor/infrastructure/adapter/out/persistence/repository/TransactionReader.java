package com.example.txprocessor.infrastructure.adapter.out.persistence.repository;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface TransactionReader {
    Mono<Transaction> findById(Long id);
    Flux<Long> findIdsByType(TransactionType type);
    Flux<Transaction> streamSubgraph(Long rootTxId);
}