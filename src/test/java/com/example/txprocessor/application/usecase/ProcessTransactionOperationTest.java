package com.example.txprocessor.application.usecase;

import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.domain.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProcessTransactionOperationTest {

    @Mock
    private TransactionService transactionService;

    private ProcessTransactionOperation operation;

    @BeforeEach
    void setUp() {
        operation = new ProcessTransactionOperation(transactionService);
    }

    @Test
    void invoke_shouldDelegateToTransactionService() {
        // given
        Long id = 10L;
        BigDecimal amount = BigDecimal.TEN;
        TransactionType type = TransactionType.cars;
        Long parentId = null;

        when(transactionService.upsertTransaction(id, amount, type, parentId))
                .thenReturn(Mono.empty());

        // when / then
        StepVerifier.create(operation.invoke(id, amount, type, parentId))
                .verifyComplete();

        verify(transactionService).upsertTransaction(id, amount, type, parentId);
    }

    @Test
    void invoke_shouldPropagateServiceError() {
        // given
        RuntimeException error = new RuntimeException("boom");

        when(transactionService.upsertTransaction(any(), any(), any(), any()))
                .thenReturn(Mono.error(error));

        // when / then
        StepVerifier.create(
                        operation.invoke(1L, BigDecimal.ONE, TransactionType.cars, null)
                )
                .expectError(RuntimeException.class)
                .verify();
    }
}