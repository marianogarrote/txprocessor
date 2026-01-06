package com.example.txprocessor.infrastructure.adapter.out.persistence.memory;

import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
@ConditionalOnProperty(name = "transaction.storage.type", havingValue = "in-memory", matchIfMissing = true)
@Slf4j
@Profile(value = {"local, test"})
public class InMemoryTransactionPort implements TransactionPort {
    private final Map<Long, Transaction> byTxId = new ConcurrentHashMap<>();
    private final Map<TransactionType, List<Long>> typeIndex = new ConcurrentHashMap<>();
    private final Map<Long, List<Long>> parentChildIndex = new ConcurrentHashMap<>();
    private final Map<Long, Long> childParentIndex = new ConcurrentHashMap<>();

    @Override
    public Mono<Transaction> findById(Long id) {
        return Mono.fromSupplier(() -> {
            Transaction tx = byTxId.get(id);
            log.debug("findById({}) -> {}", id, tx != null ? "Found" : "Not found");
            return tx;
        });
    }

    @Override
    public Flux<Long> findIdsByType(TransactionType type) {
        return Flux.defer(() -> {
            List<Long> ids = typeIndex.getOrDefault(type, Collections.emptyList());
            log.debug("findIdsByType({}) -> {} items", type, ids.size());
            return Flux.fromIterable(new ArrayList<>(ids)); // Return defensive copy
        });
    }

    @Override
    public Mono<Transaction> save(Transaction transaction) {
        return Mono.fromCallable(() -> {
            Long transactionId = transaction.id();
            Transaction existingTransaction = byTxId.get(transactionId);

            log.info("Saving - Transaction ID: {}, Amount: {}, Type: {}, Parent: {}",
                    transactionId,
                    transaction.amount(),
                    transaction.type(),
                    transaction.parent() != null ? transaction.parent().id() : null);

            // 1. Update by transaction id
            byTxId.put(transactionId, transaction);
            log.debug("Stored transaction {} in transactionStore", transactionId);

            // 2. Update by type index
            updateTypeIndex(transactionId, transaction.type(),
                    existingTransaction != null ? existingTransaction.type() : null);

            // 3. Update parent-child relationships
            updateParentChildRelationships(transactionId, transaction, existingTransaction);

            log.info("Saved - Transaction {} saved successfully", transactionId);
            log.debug(
                    "Current parentChildIndex = {}, Current childParentIndex = {}",
                    parentChildIndex,
                    childParentIndex
            );

            return transaction;
        });
    }

    private void updateTypeIndex(Long transactionId, TransactionType newType, TransactionType oldType) {
        // Remove from old type index if type changed
        if (oldType != null && !oldType.equals(newType)) {
            typeIndex.computeIfPresent(oldType, (type, ids) -> {
                ids.remove(transactionId);
                log.debug("Removed transaction {} from type index: {}", transactionId, oldType);
                return ids.isEmpty() ? null : ids;
            });
        }
        // Add to new type index
        typeIndex.computeIfAbsent(newType, k -> new CopyOnWriteArrayList<>()).add(transactionId);
        log.debug("Added transaction {} to type index: {}", transactionId, newType);
    }

    private void updateParentChildRelationships(
            Long transactionId,
            Transaction newTransaction,
            Transaction existingTransaction
    ) {
        Long newParentId = getParentIdFromTransaction(newTransaction);
        Long oldParentId = existingTransaction != null ? getParentIdFromTransaction(existingTransaction) : null;

        log.debug(
                "Updating parent-child for {}: oldParent={}, newParent={}",
                transactionId,
                oldParentId,
                newParentId
        );

        // If parent changed, remove from old parent's children
        if (oldParentId != null && !Objects.equals(oldParentId, newParentId)) {
            removeFromParentChildren(oldParentId, transactionId);
            childParentIndex.remove(transactionId);
            log.debug("Removed {} from parent {} children list", transactionId, oldParentId);
        }

        // Update with new parent
        if (newParentId != null) {
            // Add to child-parent index
            childParentIndex.put(transactionId, newParentId);
            log.debug("Added {} -> {} to childParentIndex", transactionId, newParentId);

            // Add to parent's children list
            addToParentChildren(newParentId, transactionId);
            log.debug("Added {} to parent {} children list", transactionId, newParentId);
        } else if (oldParentId != null) {
            // No new parent, just remove from old
            childParentIndex.remove(transactionId);
            log.debug("Removed {} from childParentIndex", transactionId);
        }
    }

    private Long getParentIdFromTransaction(Transaction transaction) {
        if (transaction == null || transaction.parent() == null) {
            return null;
        }
        return transaction.parent().id();
    }

    private void addToParentChildren(Long parentId, Long childId) {
        parentChildIndex.computeIfAbsent(parentId, k -> new CopyOnWriteArrayList<>()).add(childId);
    }

    private void removeFromParentChildren(Long parentId, Long childId) {
        parentChildIndex.computeIfPresent(parentId, (pid, children) -> {
            children.remove(childId);
            return children.isEmpty() ? null : children;
        });
    }

    @Override
    public Flux<Transaction> streamSubgraph(Long rootTxId) {
        return Flux.defer(() -> {
            log.info("streamSubgraph called for rootTxId: {}", rootTxId);

            // Check if root transaction exists
            if (!byTxId.containsKey(rootTxId)) {
                log.warn("Root transaction {} not found", rootTxId);
                return Flux.empty();
            }

            return Flux.create(sink -> {
                try {
                    Set<Long> visited = new HashSet<>();
                    Deque<Long> queue = new ArrayDeque<>();

                    queue.add(rootTxId);
                    visited.add(rootTxId);

                    log.debug("Starting BFS traversal from root: {}", rootTxId);

                    while (!queue.isEmpty() && !sink.isCancelled()) {
                        Long currentId = queue.poll();
                        Transaction currentTx = byTxId.get(currentId);

                        if (currentTx != null) {
                            log.debug("Emitting transaction: id={}, amount={}", currentId, currentTx.amount());
                            sink.next(currentTx);

                            List<Long> children = parentChildIndex.get(currentId);
                            if (children != null && !children.isEmpty()) {
                                log.debug("Found {} children for transaction {}", children.size(), currentId);

                                for (Long childId : children) {
                                    if (!visited.contains(childId)) {
                                        visited.add(childId);
                                        queue.add(childId);
                                        log.debug("Added child {} to queue", childId);
                                    }
                                }
                            } else {
                                log.debug("No children found for transaction {}", currentId);
                            }
                        }
                    }

                    if (!sink.isCancelled()) {
                        log.debug("BFS traversal completed for root: {}", rootTxId);
                        sink.complete();
                    }

                } catch (Exception e) {
                    log.error("Error during streamSubgraph for {}: {}", rootTxId, e.getMessage(), e);
                    if (!sink.isCancelled()) {
                        sink.error(e);
                    }
                }
            });
        });
    }

    /***
     * @TODO The class should implement Closeable
     */
    public void clearAllData() {
        byTxId.clear();
        typeIndex.clear();
        parentChildIndex.clear();
        childParentIndex.clear();
        log.info("All data cleared from InMemoryTransactionPort");
    }
}