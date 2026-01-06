package com.example.txprocessor.application.port.in;

import com.example.txprocessor.domain.model.TransactionType;
import reactor.core.publisher.Flux;

public interface SearchTxIdByTypeUseCase {
    Flux<Long> invoke(TransactionType type);
}