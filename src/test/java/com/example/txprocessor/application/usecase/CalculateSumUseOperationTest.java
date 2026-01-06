package com.example.txprocessor.application.usecase;

import com.example.txprocessor.domain.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalculateSumUseOperationTest {

    private static final Long TX_ID = 1L;

    @Mock
    private TransactionService service;

    private CalculateSumUseOperation underTest;

    @BeforeEach
    void setUp() {
        underTest = new CalculateSumUseOperation(service);
    }

    @Test
    void invoke_shouldReturnSumFromService() {
        // Given
        BigDecimal expectedSum = new BigDecimal("100.50");
        when(service.calculateSum(TX_ID)).thenReturn(Mono.just(expectedSum));

        // When
        Mono<BigDecimal> result = underTest.invoke(TX_ID);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedSum)
                .verifyComplete();
    }

    @Test
    void invoke_shouldPropagateErrorWhenServiceFails() {
        // Given
        when(service.calculateSum(TX_ID)).thenReturn(Mono.error(new RuntimeException("Service error")));

        // When
        Mono<BigDecimal> result = underTest.invoke(TX_ID);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}