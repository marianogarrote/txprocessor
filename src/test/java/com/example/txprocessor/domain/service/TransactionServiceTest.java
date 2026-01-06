package com.example.txprocessor.domain.service;

import com.example.txprocessor.application.port.out.TransactionPort;
import com.example.txprocessor.domain.exception.ParentTransactionNotFoundException;
import com.example.txprocessor.domain.exception.SelfParentTransactionException;
import com.example.txprocessor.domain.model.Transaction;
import com.example.txprocessor.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static reactor.test.StepVerifier.create;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TransactionServiceTest {

    private static final BigDecimal AMOUNT = BigDecimal.TEN;
    @Mock
    TransactionPort transactionPort;

    TransactionService service;

    @BeforeEach
    void setup() {
        service = new TransactionService(transactionPort);
    }

    @Nested
    class UpsertTransaction {

        @Test
        void fails_when_transaction_is_its_own_parent() {
            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cars, 10L))
                    .expectError(SelfParentTransactionException.class)
                    .verify();
        }

        @Test
        void fails_when_direct_parent_does_not_exist() {
            when(transactionPort.findById(99L)).thenReturn(Mono.empty());

            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cars, 99L))
                    .expectError(ParentTransactionNotFoundException.class)
                    .verify();
        }

        @Test
        void succeeds_when_parent_chain_has_no_cycle() {
            Transaction parent = new Transaction(11L, BigDecimal.ONE, TransactionType.cars, null);

            when(transactionPort.findById(11L)).thenReturn(Mono.just(parent));
            when(transactionPort.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cars, 11L))
                    .verifyComplete();
        }

        @Test
        void succeeds_when_parent_chain_has_multiple_levels() {
            Transaction root = new Transaction(1L, BigDecimal.ONE, TransactionType.cars, null);
            Transaction level1 = new Transaction(2L, BigDecimal.ONE, TransactionType.cars, root);

            when(transactionPort.findById(2L)).thenReturn(Mono.just(level1));
            when(transactionPort.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            create(service.upsertTransaction(10L, BigDecimal.TEN, TransactionType.cars, 2L))
                    .verifyComplete();
        }

        @Test
        void fails_when_save_operation_throws_exception() {
            // Given
            Transaction parent = new Transaction(11L, BigDecimal.ONE, TransactionType.cars, null);
            RuntimeException saveError = new RuntimeException("Save failed");

            when(transactionPort.findById(11L)).thenReturn(Mono.just(parent));
            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.error(saveError));

            // When & Then
            StepVerifier.create(service.upsertTransaction(10L, AMOUNT, TransactionType.cars, 11L))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(RuntimeException.class);
                        assertThat(error.getMessage()).isEqualTo("Save failed");
                    })
                    .verify();
        }

        @Test
        void fails_when_amount_is_null() {
            StepVerifier.create(service.upsertTransaction(10L, null, TransactionType.cars, null))
                    .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("amount"))
                    .verify();
        }

        @Test
        void fails_when_type_is_null() {
            StepVerifier.create(service.upsertTransaction(10L, AMOUNT, null, null))
                    .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("type"))
                    .verify();
        }

        @Test
        void fails_when_id_is_null() {
            StepVerifier.create(service.upsertTransaction(null, AMOUNT, TransactionType.cars, null))
                    .expectErrorSatisfies(error -> assertThat(error).hasMessageContaining("id"))
                    .verify();
        }

        @Test
        void success_when_parentId_is_null() {
            when(transactionPort.save(any(Transaction.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(service.upsertTransaction(10L, AMOUNT, TransactionType.cars, null))
                    .verifyComplete();
        }
    }

    @Nested
    class CalculateSum {

        @Test
        void calculates_transitive_sum_correctly() {
            Transaction root = new Transaction(10L, BigDecimal.valueOf(1000), TransactionType.cars, null);
            Transaction child1 = new Transaction(11L, BigDecimal.valueOf(500), TransactionType.cars, root);
            Transaction child2 = new Transaction(12L, BigDecimal.valueOf(250), TransactionType.cars, child1);

            when(transactionPort.streamSubgraph(10L)).thenReturn(Flux.just(root, child1, child2));

            create(service.calculateSum(10L))
                    .expectNext(BigDecimal.valueOf(1750))
                    .verifyComplete();
        }

        @Test
        void returns_zero_when_transaction_does_not_exist() {
            when(transactionPort.streamSubgraph(999L)).thenReturn(Flux.empty());

            StepVerifier.create(service.calculateSum(999L))
                    .expectNext(BigDecimal.ZERO)
                    .verifyComplete();
        }

        @Test
        void propagates_error_when_streamSubgraph_fails() {
            RuntimeException streamError = new RuntimeException("Stream error");

            when(transactionPort.streamSubgraph(10L)).thenReturn(Flux.error(streamError));

            StepVerifier.create(service.calculateSum(10L))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(RuntimeException.class);
                        assertThat(error.getMessage()).isEqualTo("Stream error");
                    })
                    .verify();
        }

        @Test
        void handles_single_transaction_without_children() {
            Transaction transaction = new Transaction(10L, BigDecimal.valueOf(1000), TransactionType.cars, null);

            when(transactionPort.streamSubgraph(10L)).thenReturn(Flux.just(transaction));

            StepVerifier.create(service.calculateSum(10L))
                    .expectNext(BigDecimal.valueOf(1000))
                    .verifyComplete();
        }

        @Test
        void handles_empty_subgraph() {
            when(transactionPort.streamSubgraph(10L)).thenReturn(Flux.empty());

            StepVerifier.create(service.calculateSum(10L))
                    .expectNext(BigDecimal.ZERO)
                    .verifyComplete();
        }
    }

    @Nested
    class FindByType {

        @Test
        void returns_all_ids_for_given_type() {
            when(transactionPort.findIdsByType(TransactionType.cars)).thenReturn(Flux.just(1L, 2L, 3L));
            create(service.findTxIds(TransactionType.cars))
                    .expectNext(1L, 2L, 3L)
                    .verifyComplete();
        }

        @Test
        void returns_empty_flux_when_no_transactions_of_type() {
            when(transactionPort.findIdsByType(TransactionType.cars)).thenReturn(Flux.empty());

            StepVerifier.create(service.findTxIds(TransactionType.cars))
                    .verifyComplete();
        }

        @Test
        void propagates_error_when_findIdsByType_fails() {
            RuntimeException findError = new RuntimeException("Find error");

            when(transactionPort.findIdsByType(TransactionType.cars)).thenReturn(Flux.error(findError));

            StepVerifier.create(service.findTxIds(TransactionType.cars))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(RuntimeException.class);
                        assertThat(error.getMessage()).isEqualTo("Find error");
                    })
                    .verify();
        }

        @Test
        void handles_empty_result_set() {
            when(transactionPort.findIdsByType(TransactionType.cars)).thenReturn(Flux.empty());

            StepVerifier.create(service.findTxIds(TransactionType.cars))
                    .expectNextCount(0)
                    .verifyComplete();
        }
    }

    @Nested
    class PrivateMethods {

        @Test
        void resolveParentTransaction_returns_empty_when_parentId_is_null() throws Exception {
            // Given
            Method method = TransactionService.class.getDeclaredMethod("resolveParentTransaction", Long.class);
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, (Object) null))
                    .verifyComplete();

            verify(transactionPort, never()).findById(anyLong());
        }

        @Test
        void resolveParentTransaction_returns_parent_when_parent_exists() throws Exception {
            // Given
            Long parentId = 1L;
            Transaction parent = new Transaction(parentId, BigDecimal.ONE, TransactionType.cars, null);

            when(transactionPort.findById(parentId)).thenReturn(Mono.just(parent));

            Method method = TransactionService.class.getDeclaredMethod("resolveParentTransaction", Long.class);
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, parentId))
                    .expectNext(parent)
                    .verifyComplete();

            verify(transactionPort, times(1)).findById(parentId);
        }

        @Test
        void resolveParentTransaction_throws_exception_when_parent_not_found() throws Exception {
            // Given
            Long parentId = 999L;

            when(transactionPort.findById(parentId)).thenReturn(Mono.empty());

            Method method = TransactionService.class.getDeclaredMethod("resolveParentTransaction", Long.class);
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, parentId))
                    .expectError(ParentTransactionNotFoundException.class)
                    .verify();

            verify(transactionPort, times(1)).findById(parentId);
        }

        @Test
        void createAndSaveTransaction_successfully_creates_and_saves() throws Exception {
            // Given
            Long transactionId = 1L;
            BigDecimal amount = BigDecimal.valueOf(1000);
            TransactionType type = TransactionType.cars;
            Transaction expectedTransaction = new Transaction(transactionId, amount, type, null);

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.just(expectedTransaction));

            Method method = TransactionService.class.getDeclaredMethod(
                    "createAndSaveTransaction",
                    Long.class, BigDecimal.class, TransactionType.class, Transaction.class
            );
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, transactionId, amount, type, null))
                    .expectNext(expectedTransaction)
                    .verifyComplete();

            verify(transactionPort, times(1)).save(any());
        }

        @Test
        void createAndSaveTransaction_propagates_save_errors() throws Exception {
            // Given
            Long transactionId = 1L;
            BigDecimal amount = BigDecimal.valueOf(1000);
            TransactionType type = TransactionType.cars;
            RuntimeException saveError = new RuntimeException("Save failed");

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.error(saveError));

            Method method = TransactionService.class.getDeclaredMethod(
                    "createAndSaveTransaction",
                    Long.class,
                    BigDecimal.class,
                    TransactionType.class,
                    Transaction.class
            );
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, transactionId, amount, type, null))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(RuntimeException.class);
                        assertThat(error.getMessage()).isEqualTo("Save failed");
                    })
                    .verify();

            verify(transactionPort, times(1)).save(any());
        }

        @Test
        void createAndSaveTransaction_with_parent_successfully_creates_and_saves() throws Exception {
            // Given
            Long transactionId = 2L;
            BigDecimal amount = BigDecimal.valueOf(2000);
            TransactionType type = TransactionType.shopping;
            Transaction parent = new Transaction(1L, BigDecimal.valueOf(1000), TransactionType.cars, null);
            Transaction expectedTransaction = new Transaction(transactionId, amount, type, parent);

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.just(expectedTransaction));

            Method method = TransactionService.class.getDeclaredMethod(
                    "createAndSaveTransaction",
                    Long.class, BigDecimal.class, TransactionType.class, Transaction.class
            );
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, transactionId, amount, type, parent))
                    .expectNext(expectedTransaction)
                    .verifyComplete();

            verify(transactionPort, times(1)).save(any());
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void handles_concurrent_operations_without_errors() {
            // Given
            Transaction parent = new Transaction(1L, BigDecimal.ONE, TransactionType.cars, null);

            when(transactionPort.findById(1L)).thenReturn(Mono.just(parent));
            when(transactionPort.save(any(Transaction.class)))
                    .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

            // When - simulate multiple concurrent saves
            Mono<Void> save1 = service.upsertTransaction(2L, AMOUNT, TransactionType.cars, 1L);
            Mono<Void> save2 = service.upsertTransaction(3L, AMOUNT, TransactionType.shopping, 1L);
            Mono<Void> save3 = service.upsertTransaction(4L, AMOUNT, TransactionType.electronics, 1L);

            // Then - all should complete successfully
            StepVerifier.create(Mono.when(save1, save2, save3))
                    .verifyComplete();

            verify(transactionPort, times(3)).save(any());
        }

        @Test
        void handles_negative_amounts() {
            // Given
            BigDecimal negativeAmount = BigDecimal.valueOf(-100);
            Transaction transaction = new Transaction(1L, negativeAmount, TransactionType.cars, null);

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

            // When & Then - Should allow negative amounts if business logic permits
            StepVerifier.create(service.upsertTransaction(1L, negativeAmount, TransactionType.cars, null))
                    .verifyComplete();
        }

        @Test
        void handles_zero_amount() {
            // Given
            BigDecimal zeroAmount = BigDecimal.ZERO;
            Transaction transaction = new Transaction(1L, zeroAmount, TransactionType.cars, null);

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

            // When & Then
            StepVerifier.create(service.upsertTransaction(1L, zeroAmount, TransactionType.cars, null))
                    .verifyComplete();
        }

        @Test
        void handles_large_amounts() {
            // Given
            BigDecimal largeAmount = new BigDecimal("9999999999.99");
            Transaction transaction = new Transaction(1L, largeAmount, TransactionType.cars, null);

            when(transactionPort.save(any(Transaction.class))).thenReturn(Mono.just(transaction));

            // When & Then
            StepVerifier.create(service.upsertTransaction(1L, largeAmount, TransactionType.cars, null))
                    .verifyComplete();
        }

        @Test
        void handles_resolveParentTransaction_when_findById_throws_exception() throws Exception {
            // Given
            Long parentId = 1L;
            RuntimeException findError = new RuntimeException("Find by ID error");

            when(transactionPort.findById(parentId)).thenReturn(Mono.error(findError));

            Method method = TransactionService.class.getDeclaredMethod("resolveParentTransaction", Long.class);
            method.setAccessible(true);

            // When & Then
            StepVerifier.create((Mono<Transaction>) method.invoke(service, parentId))
                    .expectErrorSatisfies(error -> {
                        assertThat(error).isInstanceOf(RuntimeException.class);
                        assertThat(error.getMessage()).isEqualTo("Find by ID error");
                    })
                    .verify();
        }
    }
}