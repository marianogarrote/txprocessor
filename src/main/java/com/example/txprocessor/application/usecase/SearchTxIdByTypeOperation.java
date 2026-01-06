package com.example.txprocessor.application.usecase;

import com.example.txprocessor.application.port.in.SearchTxIdByTypeUseCase;
import com.example.txprocessor.domain.model.TransactionType;
import com.example.txprocessor.domain.service.TransactionService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@AllArgsConstructor
@Slf4j
public class SearchTxIdByTypeOperation implements SearchTxIdByTypeUseCase {
    private final TransactionService service;

    @Override
    public Flux<Long> invoke(TransactionType type) {
        log.info("Searching transaction IDs for type: {}", type);

        return service.findTxIds(type)
                .doOnComplete(() ->
                        log.debug("Search completed successfully for type: {}", type))
                .doOnError(error ->
                        log.error("Failed to search transactions for type {}: {}",
                                type, error.getMessage(), error));
    }
}
