package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.out.persistence.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@AllArgsConstructor
public class TransactionQuery {
    private final TransactionRepository repository;

    public Mono<Transaction> findById(Long id) {
        return repository.findById(id);
    }

    public Flux<Long> findIdsByType(TransactionType type) {
        return repository.findIdsByType(type);
    }

    public Flux<Transaction> streamSubgraph(Long rootTxId) {
        return repository.streamSubgraph(rootTxId);
    }
}