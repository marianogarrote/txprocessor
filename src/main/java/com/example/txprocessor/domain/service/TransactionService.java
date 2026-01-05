package com.example.txprocessor.domain.service;

import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.domain.exception.ParentTransactionNotFoundException;
import com.example.txprocessor.domain.exception.SelfParentTransactionException;
import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Objects;

@AllArgsConstructor
public class TransactionService {

    private final TransactionPort transactionPort;

    public Mono<Void> upsertTransaction(
            Long id,
            BigDecimal amount,
            TransactionType type,
            Long parentId
    ) {

        if (id != null && Objects.equals(id, parentId)) {
            return Mono.error(new SelfParentTransactionException(id));
        }

        return resolveParentIfNeeded(parentId)
                .flatMap(parent ->
                        Mono.fromCallable(() -> new Transaction(id, amount, type, parent))
                )
                .switchIfEmpty(
                        Mono.fromCallable(() -> new Transaction(id, amount, type, null))
                )
                .flatMap(transactionPort::save)
                .then();
    }

    private Mono<Transaction> resolveParentIfNeeded(Long parentId) {
        if (parentId == null) {
            return Mono.empty();
        }

        return transactionPort.findById(parentId)
                .switchIfEmpty(Mono.error(new ParentTransactionNotFoundException(null, parentId)));
    }

    public Mono<BigDecimal> calculateSum(Long rootTxId) {
        return transactionPort.streamSubgraph(rootTxId)
                .map(Transaction::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Flux<Long> findTxIds(TransactionType type) {
        return transactionPort.findIdsByType(type);
    }
}