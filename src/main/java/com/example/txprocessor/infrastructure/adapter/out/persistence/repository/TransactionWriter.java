package com.example.txprocessor.infrastructure.adapter.out.persistence.repository;

import com.example.txprocessor.domain.model.Transaction;
import reactor.core.publisher.Mono;

public interface TransactionWriter {
    Mono<Transaction> save(Transaction transaction);
}