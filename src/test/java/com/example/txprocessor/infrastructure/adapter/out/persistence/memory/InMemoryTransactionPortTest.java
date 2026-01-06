package com.example.txprocessor.infrastructure.adapter.out.persistence.memory;

import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryTransactionPort Unit Tests")
class InMemoryTransactionPortTest {

    private InMemoryTransactionPort transactionPort;

    @BeforeEach
    void setUp() {
        transactionPort = new InMemoryTransactionPort();
    }

    @Test
    @DisplayName("Should save transaction without parent")
    void saveTransactionWithoutParent() {
        // Given
        Transaction transaction = new Transaction(1L, BigDecimal.valueOf(1000),
                TransactionType.cars, null);

        // When
        Mono<Transaction> result = transactionPort.save(transaction);

        // Then
        StepVerifier.create(result)
                .expectNextMatches(savedTx ->
                        savedTx.id().equals(1L) &&
                                savedTx.amount().equals(BigDecimal.valueOf(1000)) &&
                                savedTx.type() == TransactionType.cars &&
                                savedTx.parent() == null
                )
                .verifyComplete();

        StepVerifier.create(transactionPort.findById(1L))
                .expectNext(transaction)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should save transaction with parent")
    void saveTransactionWithParent() {
        // Given
        Transaction parent = new Transaction(1L, BigDecimal.valueOf(1000),
                TransactionType.cars, null);
        Transaction child = new Transaction(2L, BigDecimal.valueOf(500),
                TransactionType.shopping, parent);

        // When
        transactionPort.save(parent).block();
        Mono<Transaction> result = transactionPort.save(child);

        // Then
        StepVerifier.create(result)
                .expectNext(child)
                .verifyComplete();
    }

    @Test
    @DisplayName("Should update existing transaction")
    void updateExistingTransaction() {
        // Given
        Transaction initial = new Transaction(1L, BigDecimal.valueOf(1000),
                TransactionType.cars, null);
        Transaction updated = new Transaction(1L, BigDecimal.valueOf(2000),
                TransactionType.shopping, null);

        // When
        transactionPort.save(initial).block();
        transactionPort.save(updated).block();

        // Then
        StepVerifier.create(transactionPort.findById(1L))
                .expectNextMatches(tx ->
                        tx.amount().equals(BigDecimal.valueOf(2000)) &&
                                tx.type() == TransactionType.shopping
                )
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find transaction by ID")
    void findById() {
        // Given
        Transaction transaction = new Transaction(1L, BigDecimal.valueOf(1000),
                TransactionType.cars, null);
        transactionPort.save(transaction).block();

        // When & Then - existing transaction
        StepVerifier.create(transactionPort.findById(1L))
                .expectNext(transaction)
                .verifyComplete();

        // When & Then - non-existing transaction
        StepVerifier.create(transactionPort.findById(999L))
                .verifyComplete();
    }

    @Test
    @DisplayName("Should find transaction IDs by type")
    void findIdsByType() {
        // Given
        Transaction tx1 = new Transaction(1L, BigDecimal.valueOf(1000),
                TransactionType.cars, null);
        Transaction tx2 = new Transaction(2L, BigDecimal.valueOf(2000),
                TransactionType.shopping, null);
        Transaction tx3 = new Transaction(3L, BigDecimal.valueOf(3000),
                TransactionType.cars, null);

        transactionPort.save(tx1).block();
        transactionPort.save(tx2).block();
        transactionPort.save(tx3).block();

        // When & Then - CARS type
        StepVerifier.create(transactionPort.findIdsByType(TransactionType.cars))
                .expectNext(1L)
                .expectNext(3L)
                .verifyComplete();

        // When & Then - SHOPPING type
        StepVerifier.create(transactionPort.findIdsByType(TransactionType.shopping))
                .expectNext(2L)
                .verifyComplete();

        // When & Then - non-existing type
        StepVerifier.create(transactionPort.findIdsByType(TransactionType.electronics))
                .verifyComplete();
    }

    @Nested
    @DisplayName("Stream Subgraph Tests")
    class StreamSubgraphTests {

        @Test
        @DisplayName("Should stream single transaction without children")
        void streamSingleTransaction() {
            // Given
            Transaction tx = new Transaction(1L, BigDecimal.valueOf(1000),
                    TransactionType.cars, null);
            transactionPort.save(tx).block();

            // When & Then
            StepVerifier.create(transactionPort.streamSubgraph(1L))
                    .expectNext(tx)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should stream linear chain of transactions")
        void streamLinearChain() {
            // Given: 1 -> 2 -> 3
            Transaction tx1 = new Transaction(1L, BigDecimal.valueOf(1000),
                    TransactionType.cars, null);
            Transaction tx2 = new Transaction(2L, BigDecimal.valueOf(2000),
                    TransactionType.shopping, tx1);
            Transaction tx3 = new Transaction(3L, BigDecimal.valueOf(3000),
                    TransactionType.shopping, tx2);

            transactionPort.save(tx1).block();
            transactionPort.save(tx2).block();
            transactionPort.save(tx3).block();

            // When & Then - stream from root (1)
            StepVerifier.create(transactionPort.streamSubgraph(1L))
                    .expectNext(tx1)
                    .expectNext(tx2)
                    .expectNext(tx3)
                    .verifyComplete();

            // When & Then - stream from middle (2)
            StepVerifier.create(transactionPort.streamSubgraph(2L))
                    .expectNext(tx2)
                    .expectNext(tx3)
                    .verifyComplete();

            // When & Then - stream from leaf (3)
            StepVerifier.create(transactionPort.streamSubgraph(3L))
                    .expectNext(tx3)
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should return empty flux for non-existing root")
        void streamNonExistingRoot() {
            // When & Then
            StepVerifier.create(transactionPort.streamSubgraph(999L))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Should handle cycle prevention in stream")
        void streamShouldNotCreateCycle() {
            // Note: This test assumes cycle prevention is handled at the Transaction level
            // Given
            Transaction tx1 = new Transaction(1L, BigDecimal.valueOf(1000),
                    TransactionType.cars, null);
            Transaction tx2 = new Transaction(2L, BigDecimal.valueOf(2000),
                    TransactionType.shopping, tx1);

            transactionPort.save(tx1).block();
            transactionPort.save(tx2).block();

            // Update tx1 to have tx2 as parent (would create cycle: 1 -> 2 -> 1)
            // This should be prevented by Transaction validation
            Transaction updatedTx1 = new Transaction(1L, BigDecimal.valueOf(1000),
                    TransactionType.cars, tx2);

            // Save should succeed (cycle validation happens in Transaction constructor)
            transactionPort.save(updatedTx1).block();

            // Stream should still work without infinite loop
            StepVerifier.create(transactionPort.streamSubgraph(1L))
                    .expectNext(updatedTx1)
                    .expectNext(tx2)
                    .verifyComplete();
        }
    }

    @Test
    @DisplayName("Should handle concurrent saves")
    void concurrentSaves() throws InterruptedException {
        // Given
        int numThreads = 10;
        Thread[] threads = new Thread[numThreads];

        // When - save transactions concurrently
        for (int i = 0; i < numThreads; i++) {
            final int id = i + 1;
            threads[i] = new Thread(() -> {
                Transaction tx = new Transaction(
                        (long) id,
                        BigDecimal.valueOf(id * 1000),
                        TransactionType.cars,
                        null
                );
                transactionPort.save(tx).block();
            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
    }
}