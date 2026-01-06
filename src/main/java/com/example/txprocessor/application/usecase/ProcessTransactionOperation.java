package com.example.txprocessor.application.usecase;

import com.example.txprocessor.application.port.in.ProcessTransactionUseCase;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.domain.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@AllArgsConstructor
@Slf4j
public class ProcessTransactionOperation implements ProcessTransactionUseCase {
    private final TransactionService service;

    @Override
    public Mono<Void> invoke(Long id, BigDecimal amount, TransactionType type, Long parentId) {
        log.debug(
                "Creating/Updating data for transaction id = {}, amount = {}, type = {}, parentId = {}",
                id,
                amount,
                type,
                parentId
        );

        return service.upsertTransaction(id, amount, type, parentId)
                .doOnSuccess(unused ->
                        log.info("Transaction {} processed successfully via use case", id))
                .doOnError(error ->
                        log.error("Failed to process transaction {} via use case: {}",
                                id, error.getMessage(), error));
    }
}
