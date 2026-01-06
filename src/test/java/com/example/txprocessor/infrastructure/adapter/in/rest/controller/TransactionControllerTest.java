package com.example.txprocessor.infrastructure.adapter.in.rest.controller;

import com.example.txprocessor.application.port.in.CalculateSumUseCase;
import com.example.txprocessor.application.port.in.ProcessTransactionUseCase;
import com.example.txprocessor.application.port.in.SearchTxIdByTypeUseCase;
import com.example.txprocessor.domain.model.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class TransactionControllerTest {

    @Mock
    private ProcessTransactionUseCase processTransactionUseCase;

    @Mock
    private SearchTxIdByTypeUseCase searchTxIdByTypeUseCase;

    @Mock
    private CalculateSumUseCase calculateSumUseCase;

    private TransactionController controller;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        controller = new TransactionController(
                processTransactionUseCase,
                calculateSumUseCase,
                searchTxIdByTypeUseCase
        );
        webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void validate_shouldReturnValidResponseWhenValidationSucceeds() {

        when(processTransactionUseCase.invoke(
                eq(12L),
                any(BigDecimal.class),
                eq(TransactionType.cars),
                isNull()
        )).thenReturn(Mono.empty());

        String requestBody = """
            {
                "amount": 5000,
                "type": "cars"
            }
            """;

        webTestClient.put()
                .uri("/api/v1/transactions/12")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }

    @Test
    void validate_shouldReturnFailureResponseWhenUseCaseFails() {
        // Given
        when(processTransactionUseCase.invoke(
                eq(12L),
                any(BigDecimal.class),
                eq(TransactionType.cars),
                isNull()
        )).thenReturn(Mono.error(new RuntimeException("Test error")));

        String requestBody = """
                {
                    "amount": 5000,
                    "type": "cars"
                }
                """;

        // When & Then
        webTestClient.put()
                .uri("/api/v1/transactions/12")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void getByType_shouldReturnIdsForValidType() {
        // Given
        when(searchTxIdByTypeUseCase.invoke(eq(TransactionType.cars)))
                .thenReturn(Flux.just(10L, 20L, 30L));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/transactions/types/cars")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0]").isEqualTo(10)
                .jsonPath("$[1]").isEqualTo(20)
                .jsonPath("$[2]").isEqualTo(30);
    }

    @Test
    void getByType_shouldReturnEmptyListForTypeWithoutTransactions() {
        // Given
        when(searchTxIdByTypeUseCase.invoke(any(TransactionType.class)))
                .thenReturn(Flux.empty());

        // When & Then
        webTestClient.get()
                .uri("/api/v1/transactions/types/electronics")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    void calculateSum_shouldReturnSumForValidTransaction() {
        // Given
        when(calculateSumUseCase.invoke(eq(10L)))
                .thenReturn(Mono.just(BigDecimal.valueOf(1500.0)));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/transactions/sum/10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sum").isEqualTo(1500.0);
    }

    @Test
    void calculateSum_shouldReturnZeroForNonExistentTransaction() {
        // Given
        when(calculateSumUseCase.invoke(eq(999L)))
                .thenReturn(Mono.just(BigDecimal.ZERO));

        // When & Then
        webTestClient.get()
                .uri("/api/v1/transactions/sum/999")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.sum").isEqualTo(0.0);
    }

    @Test
    void validate_shouldHandleTransactionWithParent() {

        when(processTransactionUseCase.invoke(
                eq(11L),
                eq(new BigDecimal(10000)),
                eq(TransactionType.shopping),
                eq(10L)
        )).thenReturn(Mono.empty());

        String requestBody = """
            {
                "amount": 10000,
                "type": "shopping",
                "parent_id": 10
            }
            """;

        webTestClient.put()
                .uri("/api/v1/transactions/11")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }
}