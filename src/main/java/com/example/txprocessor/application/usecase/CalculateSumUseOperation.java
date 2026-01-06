package com.example.txprocessor.application.usecase;

import com.example.txprocessor.application.port.in.CalculateSumUseCase;
import com.example.txprocessor.domain.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;

@AllArgsConstructor
@Slf4j
public class CalculateSumUseOperation implements CalculateSumUseCase {
    private final TransactionService service;

    public Mono<BigDecimal> invoke(Long txId) {
        log.debug("Calculation amount sum for transaction graph with root id = " + txId);
        return service.calculateSum(txId)
                .doOnSuccess(sum ->
                        log.info("Successfully calculated sum for transaction {}: {}", txId, sum))
                .doOnError(error ->
                        log.error("Error calculating sum for transaction {}: {}", txId,
                                error.getMessage(), error));
    }
}
