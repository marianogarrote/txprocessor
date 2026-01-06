package com.example.txprocessor.application.usecase;

import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.domain.service.TransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchTxIdByTypeOperationTest {

    @Mock
    private TransactionService service;

    private SearchTxIdByTypeOperation underTest;

    @BeforeEach
    void setUp() {
        underTest = new SearchTxIdByTypeOperation(service);
    }

    @Test
    void invoke_shouldReturnFluxOfIdsFromService() {
        // Given
        TransactionType type = TransactionType.cars;
        List<Long> expectedIds = Arrays.asList(1L, 2L, 3L);
        when(service.findTxIds(type)).thenReturn(Flux.fromIterable(expectedIds));

        // When
        Flux<Long> result = underTest.invoke(type);

        // Then
        StepVerifier.create(result)
                .expectNextSequence(expectedIds)
                .verifyComplete();
    }

    @Test
    void invoke_shouldReturnEmptyFluxWhenNoTransactionsOfType() {
        // Given
        TransactionType type = TransactionType.cars;
        when(service.findTxIds(type)).thenReturn(Flux.empty());

        // When
        Flux<Long> result = underTest.invoke(type);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void invoke_shouldPropagateErrorWhenServiceFails() {
        // Given
        TransactionType type = TransactionType.cars;
        when(service.findTxIds(type)).thenReturn(Flux.error(new RuntimeException("Service error")));

        // When
        Flux<Long> result = underTest.invoke(type);

        // Then
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }


    @Test
    void invoke_happyPath() {
        TransactionType type = TransactionType.cars;
        when(service.findTxIds(type)).thenReturn(Flux.just(1L, 2L));

        StepVerifier.create(underTest.invoke(type))
                .expectNextCount(2)
                .verifyComplete();
    }
}