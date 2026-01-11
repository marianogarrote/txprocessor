package com.example.txprocessor.infrastructure.adapter.out.persistence.cqrs;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.infrastructure.adapter.out.persistence.repository.TransactionWriter;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@AllArgsConstructor
public class TransactionCommand {
    private final TransactionWriter repository;
    public Mono<Transaction> save(Transaction transaction) {
        return repository.save(transaction);
    }
}
