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
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
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
            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cards, 10L))
                    .expectError(SelfParentTransactionException.class)
                    .verify();
        }

        @Test
        void fails_when_direct_parent_does_not_exist() {
            when(transactionPort.findById(99L)).thenReturn(Mono.empty());

            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cards, 99L))
                    .expectError(ParentTransactionNotFoundException.class)
                    .verify();
        }

        @Test
        void succeeds_when_parent_chain_has_no_cycle() {
            Transaction parent = new Transaction(11L, BigDecimal.ONE, TransactionType.cards, null);

            when(transactionPort.findById(11L)).thenReturn(Mono.just(parent));
            when(transactionPort.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            create(service.upsertTransaction(10L, AMOUNT, TransactionType.cards, 11L))
                    .verifyComplete();
        }

        @Test
        void succeeds_when_parent_chain_has_multiple_levels() {
            Transaction root = new Transaction(1L, BigDecimal.ONE, TransactionType.cards, null);
            Transaction level1 = new Transaction(2L, BigDecimal.ONE, TransactionType.cards, root);
            Transaction level2 = new Transaction(3L, BigDecimal.ONE, TransactionType.cards, level1);

            when(transactionPort.findById(2L)).thenReturn(Mono.just(level1));
            when(transactionPort.save(any())).thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            create(service.upsertTransaction(10L,BigDecimal.TEN,TransactionType.cards,2L))
                    .verifyComplete();
        }
    }

    @Nested
    class CalculateSum {

        @Test
        void calculates_transitive_sum_correctly() {
            Transaction root = new Transaction(10L, BigDecimal.valueOf(1000), TransactionType.cards, null);
            Transaction child1 = new Transaction(11L, BigDecimal.valueOf(500), TransactionType.cards, root);
            Transaction child2 = new Transaction(12L, BigDecimal.valueOf(250), TransactionType.cards, child1);

            when(transactionPort.streamSubgraph(10L)).thenReturn(Flux.just(root, child1, child2));

            create(service.calculateSum(10L))
                    .expectNext(BigDecimal.valueOf(1750))
                    .verifyComplete();
        }
    }

    @Nested
    class FindByType {

        @Test
        void returns_all_ids_for_given_type() {
            when(transactionPort.findIdsByType(TransactionType.cards)).thenReturn(Flux.just(1L, 2L, 3L));
            create(service.findTxIds(TransactionType.cards))
                    .expectNext(1L, 2L, 3L)
                    .verifyComplete();
        }
    }

}