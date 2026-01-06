package com.example.txprocessor.infrastructure.adapter.in.rest.controller;

import com.example.txprocessor.application.port.in.CalculateSumUseCase;
import com.example.txprocessor.application.port.in.ProcessTransactionUseCase;
import com.example.txprocessor.application.port.in.SearchTxIdByTypeUseCase;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.infrastructure.adapter.in.rest.controller.documentation.TransactionApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/transactions")
public class TransactionController implements TransactionApi {

    private final ProcessTransactionUseCase processTransactionUseCase;
    private final CalculateSumUseCase calculateSumUseCase;
    private final SearchTxIdByTypeUseCase searchTxIdByTypeUseCase;

    public Mono<TransactionApi.ProcessTransactionOutput> processTransaction(
            @PathVariable("transaction_id") Long transactionId,
            @Validated @RequestBody ProcessTransactionInput input
    ) {
        log.info("PUT /transactions/{} - Request: {}", transactionId, input);

        return processTransactionUseCase
                .invoke(transactionId, input.amount(), input.type(), input.parentId())
                .thenReturn(ProcessTransactionOutput.ok())
                .doOnSuccess(output ->
                        log.info("Transaction {} processed successfully", transactionId)
                )
                .doOnError(error ->
                        log.error("Error processing transaction {}: {}", transactionId, error.getMessage())
                );
    }

    @GetMapping("/types/{type}")
    public Flux<Long> searchByType(@Validated @PathVariable("type") TransactionType type) {
        long startTime = System.currentTimeMillis();
        log.info("GET /transactions/types/{} - Starting search", type);

        return searchTxIdByTypeUseCase.invoke(type)
                .doOnSubscribe(subscription -> log.debug("Type search subscription started for: {}", type))
                .doOnNext(id -> log.trace("Found transaction ID: {} for type: {}", id, type))
                .collectList()
                .doOnSuccess(ids -> {
                    long duration = System.currentTimeMillis() - startTime;
                    if (ids == null || ids.isEmpty()) {
                        log.warn("GET /transactions/types/{} - No transactions found ({} ms)", type, duration);
                    } else {
                        log.info("GET /transactions/types/{} - Found {} transactions in {} ms",type, ids.size(), duration);
                    }
                })
                .doOnError(error -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.error(
                            "GET /transactions/types/{} - Error after {} ms: {}",
                            type,
                            duration,
                            error.getMessage(),
                            error
                    );
                })
                .flatMapMany(Flux::fromIterable);
    }

    @GetMapping("/sum/{transaction_id}")
    public Mono<TransactionApi.SumOutput> calculateSum(@Validated @PathVariable("transaction_id") Long transactionId) {
        log.info("GET /transactions/sum/{}", transactionId);
        return calculateSumUseCase.invoke(transactionId)
                .map(SumOutput::new)
                .doOnSuccess(output -> log.info("Sum calculated for {}: {}", transactionId, output.sum()))
                .doOnError(error ->
                        log.error("Error calculating sum for {}: {}", transactionId, error.getMessage())
                );
    }
}