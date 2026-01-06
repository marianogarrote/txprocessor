package com.example.txprocessor.domain.service;

import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.domain.exception.ParentTransactionNotFoundException;
import com.example.txprocessor.domain.exception.SelfParentTransactionException;
import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;

@AllArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionPort transactionPort;

    public Mono<Void> upsertTransaction(
            Long id,
            BigDecimal amount,
            TransactionType type,
            Long parentId
    ) {
        log.info("=== UPSERT TRANSACTION START ===");
        log.info("ID: {}, Amount: {}, Type: {}, ParentID: {}", id, amount, type, parentId);

        if (id != null && Objects.equals(id, parentId)) {
            log.error("Self-parent transaction attempted for id: {}", id);
            return Mono.error(new SelfParentTransactionException(id));
        }

        return resolveParentTransaction(parentId)
                .flatMap(parent -> {
                    log.info(
                            "Parent resolved for transaction {}: {}",
                            id,
                            parent != null ? "ID=" + parent.id() : "null"
                    );
                    return createAndSaveTransaction(id, amount, type, parent);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info(
                            "No parent specified for transaction {}, creating without parent",
                            id
                    );
                    return createAndSaveTransaction(id, amount, type, null);
                }))
                .then()
                .doOnError(error ->
                        log.error("Error saving transaction {}: {}", id, error.getMessage(), error))
                .doOnTerminate(() ->
                        log.info("=== UPSERT TRANSACTION END ==="));
    }

    private Mono<Transaction> resolveParentTransaction(Long parentId) {
        if (parentId == null) {
            log.debug("No parentId to resolve");
            return Mono.empty();
        }

        log.debug("Resolving parent transaction with id: {}", parentId);
        return transactionPort.findById(parentId)
                .switchIfEmpty(Mono.defer(() -> {
                    log.error("Parent transaction not found: {}", parentId);
                    return Mono.error(new ParentTransactionNotFoundException(null, parentId));
                }))
                .doOnSuccess(parent ->
                        log.debug("Successfully resolved parent transaction: {}", parent.id()))
                .doOnError(error ->
                        log.error("Failed to resolve parent {}: {}", parentId, error.getMessage()));
    }

    private Mono<Transaction> createAndSaveTransaction(
            Long id,
            BigDecimal amount,
            TransactionType type,
            Transaction parent
    ) {
        return Mono.fromCallable(() -> {
                    log.debug("Creating Transaction object: id={}, parent={}", id, parent != null ? parent.id() : null);

                    Transaction transaction = new Transaction(id, amount, type, parent);
                    Long txParentId = transaction.parent() != null ? transaction.parent().id() : null;
                    log.debug("Transaction created = {}, parent object({}) = {}", transaction, txParentId, transaction.parent());
                    return transaction;
                })
                .flatMap(transactionPort::save)
                .doOnSuccess(savedTx -> log.debug("Transaction saved to port: {}", savedTx.id()));
    }

    public Mono<BigDecimal> calculateSum(Long rootTxId) {
        log.info("=== CALCULATE SUM START ===");
        log.info("Calculating sum for transaction: {}", rootTxId);

        return transactionPort.streamSubgraph(rootTxId)
                .doOnSubscribe(subscription -> log.debug("Starting subgraph traversal"))
                .collectList()
                .doOnNext(transactions -> {
                    log.debug("Found {} transactions in subgraph:", transactions.size());
                    transactions.forEach(tx ->
                            log.debug(
                                    "  - Id = {}, Amount = {}, Parent = {}",
                                    tx.id(),
                                    tx.amount(),
                                    tx.parent() != null ? tx.parent().id() : null
                            )
                    );
                })
                .flatMap(transactions -> {
                    BigDecimal sum = transactions.stream()
                            .map(Transaction::amount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

                    log.info("Calculated sum for transaction {}: {}", rootTxId, sum);
                    log.info("=== CALCULATE SUM END ===");
                    return Mono.just(sum);
                })
                .doOnError(error -> {
                    log.error("Error calculating sum for transaction {}: {}", rootTxId, error.getMessage(), error);
                    log.info("=== CALCULATE SUM END (ERROR) ===");
                });
    }

    public Flux<Long> findTxIds(TransactionType type) {
        log.info("Finding transaction IDs for type: {}", type);
        return transactionPort.findIdsByType(type)
                .doOnNext(id -> log.trace("Found transaction Id = {} for type = {}", id, type))
                .collectList()
                .doOnNext(ids -> log.info("Found {} transactions for type = {}", ids.size(), type))
                .flatMapMany(Flux::fromIterable);
    }
}